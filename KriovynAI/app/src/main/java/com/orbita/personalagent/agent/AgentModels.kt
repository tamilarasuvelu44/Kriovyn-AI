package com.orbita.personalagent.agent

import com.orbita.personalagent.ai.MemoryCandidate

enum class ToolRiskLevel(val label: String) {
    LOW_LOCAL("Local Safe"),
    EXTERNAL_INTENT("External Action — Approval Required")
}

sealed interface ToolResult {
    val summary: String

    data class Success(override val summary: String, val details: String = "") : ToolResult
    data class Failure(override val summary: String, val reason: String) : ToolResult
    data class Rejected(override val summary: String) : ToolResult
    data class Cancelled(override val summary: String) : ToolResult
    data class TimedOut(override val summary: String) : ToolResult
}

data class AgentStep(
    val stepIndex: Int,
    val toolId: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val rationale: String,
    val riskLevel: ToolRiskLevel,
    val requiresApproval: Boolean
)

data class AgentPlan(
    val planId: String,
    val sessionId: Long,
    val userPrompt: String,
    val steps: List<AgentStep>
)

sealed interface AgentState {
    data object Idle : AgentState
    data class Planning(val userPrompt: String) : AgentState
    data class AwaitingApproval(val request: ToolApprovalRequest) : AgentState
    data class ExecutingTool(val step: AgentStep, val totalSteps: Int) : AgentState
    data object Synthesizing : AgentState
    data class Completed(
        val assistantReply: String,
        val memoryCandidates: List<MemoryCandidate>
    ) : AgentState

    data class Error(val message: String, val recoverablePrompt: String) : AgentState
}

sealed interface ArgumentValidation {
    data object Valid : ArgumentValidation
    data class Invalid(val reason: String) : ArgumentValidation
}

interface AgentTool {
    val id: String
    val displayName: String
    val description: String
    val riskLevel: ToolRiskLevel
    val requiresApproval: Boolean
    val targetActionSummary: String

    fun validateArguments(arguments: Map<String, String>): ArgumentValidation
    suspend fun execute(arguments: Map<String, String>): ToolResult
}
