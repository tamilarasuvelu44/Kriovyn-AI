package com.orbita.personalagent

import com.orbita.personalagent.agent.AgentTool
import com.orbita.personalagent.agent.ArgumentValidation
import com.orbita.personalagent.agent.PolicyEvaluation
import com.orbita.personalagent.agent.ToolPolicyEngine
import com.orbita.personalagent.agent.ToolRegistry
import com.orbita.personalagent.agent.ToolResult
import com.orbita.personalagent.agent.ToolRiskLevel
import com.orbita.personalagent.ai.ProposedToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPolicyEngineTest {

    private val dummyTool = object : AgentTool {
        override val id = "save_local_note"
        override val displayName = "Save Local Note"
        override val description = "Save note"
        override val riskLevel = ToolRiskLevel.LOW_LOCAL
        override val requiresApproval = false
        override val targetActionSummary = "Local DB write"

        override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
            return if (arguments["content"].isNullOrBlank()) {
                ArgumentValidation.Invalid("Content required")
            } else {
                ArgumentValidation.Valid
            }
        }

        override suspend fun execute(arguments: Map<String, String>) = ToolResult.Success("Saved")
    }

    private val engine = ToolPolicyEngine(ToolRegistry(listOf(dummyTool)))

    @Test
    fun blocksNonAllowlistedTool() {
        val eval = engine.evaluatePlan(
            planId = "p1",
            sessionId = 1L,
            userPrompt = "Run shell",
            proposedCalls = listOf(
                ProposedToolCall("exec_arbitrary_code", mapOf("cmd" to "ls"), "hack")
            )
        )
        assertTrue(eval is PolicyEvaluation.Blocked)
    }

    @Test
    fun enforcesMaxFiveToolsPerRequest() {
        val sevenCalls = (1..7).map { idx ->
            ProposedToolCall("save_local_note", mapOf("content" to "Note $idx"), "save")
        }
        val eval = engine.evaluatePlan("p2", 1L, "Save 7 notes", sevenCalls)
        assertTrue(eval is PolicyEvaluation.Allowed)
        val allowed = eval as PolicyEvaluation.Allowed
        assertEquals(5, allowed.plan.steps.size)
        assertTrue(allowed.warnings.any { it.contains("capped tool execution at 5") })
    }

    @Test
    fun blocksForbiddenSystemSettingsOrPaymentPayloads() {
        val eval = engine.evaluatePlan(
            planId = "p3",
            sessionId = 1L,
            userPrompt = "Pay",
            proposedCalls = listOf(
                ProposedToolCall("save_local_note", mapOf("content" to "upi://pay?pa=test"), "bad")
            )
        )
        assertTrue(eval is PolicyEvaluation.Blocked)
    }

    @Test
    fun rejectsInvalidArguments() {
        val eval = engine.evaluatePlan(
            planId = "p4",
            sessionId = 1L,
            userPrompt = "Empty note",
            proposedCalls = listOf(
                ProposedToolCall("save_local_note", mapOf("content" to "   "), "empty")
            )
        )
        assertTrue(eval is PolicyEvaluation.Blocked)
    }
}
