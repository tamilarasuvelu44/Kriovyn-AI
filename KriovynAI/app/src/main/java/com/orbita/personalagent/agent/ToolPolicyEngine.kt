package com.orbita.personalagent.agent

import com.orbita.personalagent.ai.ProposedToolCall

sealed interface PolicyEvaluation {
    data class Allowed(val plan: AgentPlan, val warnings: List<String>) : PolicyEvaluation
    data class Blocked(val reason: String) : PolicyEvaluation
}

class ToolPolicyEngine(private val toolRegistry: ToolRegistry) {

    fun evaluatePlan(
        planId: String,
        sessionId: Long,
        userPrompt: String,
        proposedCalls: List<ProposedToolCall>
    ): PolicyEvaluation {
        if (proposedCalls.isEmpty()) {
            return PolicyEvaluation.Allowed(
                plan = AgentPlan(planId, sessionId, userPrompt, emptyList()),
                warnings = emptyList()
            )
        }

        val warnings = mutableListOf<String>()
        val boundedCalls = if (proposedCalls.size > MAX_TOOLS_PER_REQUEST) {
            warnings.add("Safety limit applied: capped tool execution at $MAX_TOOLS_PER_REQUEST steps.")
            proposedCalls.take(MAX_TOOLS_PER_REQUEST)
        } else {
            proposedCalls
        }

        val validatedSteps = mutableListOf<AgentStep>()
        boundedCalls.forEachIndexed { index, call ->
            val tool = toolRegistry.getTool(call.toolId)
            if (tool == null) {
                warnings.add("Blocked non-allowlisted tool '${call.toolId}'.")
                return@forEachIndexed
            }

            if (containsForbiddenAction(call.arguments)) {
                warnings.add("Blocked '${tool.displayName}' due to protected action policy.")
                return@forEachIndexed
            }

            when (val validation = tool.validateArguments(call.arguments)) {
                is ArgumentValidation.Invalid -> {
                    warnings.add("Skipped '${tool.displayName}': ${validation.reason}")
                }
                ArgumentValidation.Valid -> {
                    validatedSteps.add(
                        AgentStep(
                            stepIndex = index + 1,
                            toolId = tool.id,
                            toolName = tool.displayName,
                            arguments = call.arguments,
                            rationale = call.rationale.ifBlank { tool.description },
                            riskLevel = tool.riskLevel,
                            requiresApproval = tool.requiresApproval
                        )
                    )
                }
            }
        }

        return if (validatedSteps.isEmpty() && warnings.isNotEmpty()) {
            PolicyEvaluation.Blocked(warnings.joinToString("\n"))
        } else {
            PolicyEvaluation.Allowed(
                plan = AgentPlan(planId, sessionId, userPrompt, validatedSteps),
                warnings = warnings
            )
        }
    }

    private fun containsForbiddenAction(arguments: Map<String, String>): Boolean {
        val joined = arguments.values.joinToString(" ").lowercase()
        val forbiddenMarkers = listOf(
            "android.settings.",
            "action_call",
            "upi://",
            "paytm://",
            "su ",
            "rm -rf"
        )
        return forbiddenMarkers.any { joined.contains(it) }
    }

    companion object {
        const val MAX_TOOLS_PER_REQUEST = 5
        const val TOOL_TIMEOUT_MS = 10_000L
    }
}
