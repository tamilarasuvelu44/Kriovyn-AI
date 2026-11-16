package com.orbita.personalagent.agent

import com.orbita.personalagent.ai.AiProviderManager
import com.orbita.personalagent.ai.AiRequest
import com.orbita.personalagent.ai.MemoryCandidate
import com.orbita.personalagent.data.local.db.MessageRole
import com.orbita.personalagent.data.preferences.SettingsRepository
import com.orbita.personalagent.data.repository.ChatRepository
import com.orbita.personalagent.data.repository.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.util.UUID

class AgentOrchestrator(
    private val aiProviderManager: AiProviderManager,
    private val toolRegistry: ToolRegistry,
    private val toolPolicyEngine: ToolPolicyEngine,
    private val approvalManager: ApprovalManager,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository
) {
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    suspend fun processUserMessage(
        sessionId: Long,
        userInput: String,
        persistUserMessage: Boolean = true
    ): List<MemoryCandidate> {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        if (persistUserMessage) {
            chatRepository.appendMessage(
                sessionId = sessionId,
                role = MessageRole.USER,
                content = trimmed
            )
        }

        _agentState.value = AgentState.Planning(trimmed)

        return try {
            val prefs = settingsRepository.getSnapshot()
            val history = chatRepository.getMessagesSnapshot(sessionId)
            val memories = memoryRepository.getActiveMemories()
            val provider = aiProviderManager.getActiveProvider()

            val aiResponse = provider.planAndRespond(
                AiRequest(
                    sessionId = sessionId,
                    userPrompt = trimmed,
                    conversationHistory = history,
                    approvedMemories = memories,
                    memorySuggestionsEnabled = prefs.memorySuggestionsEnabled
                )
            )

            val evaluation = toolPolicyEngine.evaluatePlan(
                planId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                userPrompt = trimmed,
                proposedCalls = aiResponse.proposedToolCalls
            )

            val stepResults = mutableListOf<Pair<AgentStep, ToolResult>>()
            val warnings = mutableListOf<String>()

            when (evaluation) {
                is PolicyEvaluation.Blocked -> {
                    warnings.add(evaluation.reason)
                    approvalManager.recordAudit(
                        toolId = "policy_engine",
                        toolName = "Policy Engine",
                        arguments = emptyMap(),
                        status = "BLOCKED",
                        detail = evaluation.reason
                    )
                }
                is PolicyEvaluation.Allowed -> {
                    warnings.addAll(evaluation.warnings)
                    val steps = evaluation.plan.steps
                    for (step in steps) {
                        val result = executeStepSafely(sessionId, step, steps.size)
                        stepResults.add(step to result)
                    }
                }
            }

            _agentState.value = AgentState.Synthesizing

            val finalText = buildFinalResponse(
                baseMessage = aiResponse.assistantMessage,
                stepResults = stepResults,
                warnings = warnings
            )

            val toolSummary = if (stepResults.isNotEmpty() && prefs.showToolStepDetails) {
                stepResults.joinToString(" • ") { (step, res) ->
                    val statusTag = when (res) {
                        is ToolResult.Success -> "OK"
                        is ToolResult.Rejected -> "Rejected"
                        is ToolResult.Cancelled -> "Cancelled"
                        is ToolResult.TimedOut -> "Timed Out"
                        is ToolResult.Failure -> "Failed"
                    }
                    "${step.toolName}: $statusTag"
                }
            } else {
                null
            }

            chatRepository.appendMessage(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = finalText,
                toolSummary = toolSummary
            )

            _agentState.value = AgentState.Completed(
                assistantReply = finalText,
                memoryCandidates = aiResponse.memoryCandidates
            )
            aiResponse.memoryCandidates
        } catch (ce: CancellationException) {
            approvalManager.cancelAllPending()
            _agentState.value = AgentState.Idle
            throw ce
        } catch (ex: Exception) {
            val errorText = ex.localizedMessage ?: "An unexpected error occurred while processing your request."
            chatRepository.appendMessage(
                sessionId = sessionId,
                role = MessageRole.ERROR,
                content = errorText,
                isError = true
            )
            _agentState.value = AgentState.Error(
                message = errorText,
                recoverablePrompt = trimmed
            )
            emptyList()
        }
    }

    private suspend fun executeStepSafely(
        sessionId: Long,
        step: AgentStep,
        totalSteps: Int
    ): ToolResult {
        val tool = toolRegistry.getTool(step.toolId)
            ?: return ToolResult.Failure("Tool missing", "Tool '${step.toolId}' is not in the allowlist.")

        if (step.requiresApproval) {
            val request = ToolApprovalRequest(
                sessionId = sessionId,
                step = step,
                targetActionSummary = tool.targetActionSummary
            )
            _agentState.value = AgentState.AwaitingApproval(request)

            return when (approvalManager.requestApproval(request)) {
                ApprovalDecision.REJECTED -> {
                    val result = ToolResult.Rejected("User rejected '${step.toolName}'.")
                    approvalManager.recordAudit(
                        step.toolId,
                        step.toolName,
                        step.arguments,
                        "REJECTED",
                        result.summary
                    )
                    result
                }
                ApprovalDecision.CANCELLED -> {
                    val result = ToolResult.Cancelled("Cancelled '${step.toolName}'.")
                    approvalManager.recordAudit(
                        step.toolId,
                        step.toolName,
                        step.arguments,
                        "CANCELLED",
                        result.summary
                    )
                    result
                }
                ApprovalDecision.APPROVED -> {
                    runToolWithTimeout(step, totalSteps, tool)
                }
            }
        }

        return runToolWithTimeout(step, totalSteps, tool)
    }

    private suspend fun runToolWithTimeout(
        step: AgentStep,
        totalSteps: Int,
        tool: AgentTool
    ): ToolResult {
        _agentState.value = AgentState.ExecutingTool(step, totalSteps)
        return try {
            val result = withTimeout(ToolPolicyEngine.TOOL_TIMEOUT_MS) {
                tool.execute(step.arguments)
            }
            val status = when (result) {
                is ToolResult.Success -> "EXECUTED"
                is ToolResult.Failure -> "FAILED"
                is ToolResult.Rejected -> "REJECTED"
                is ToolResult.Cancelled -> "CANCELLED"
                is ToolResult.TimedOut -> "TIMED_OUT"
            }
            approvalManager.recordAudit(step.toolId, step.toolName, step.arguments, status, result.summary)
            result
        } catch (_: TimeoutCancellationException) {
            val timedOut = ToolResult.TimedOut("'${step.toolName}' timed out after ${ToolPolicyEngine.TOOL_TIMEOUT_MS / 1000}s.")
            approvalManager.recordAudit(step.toolId, step.toolName, step.arguments, "TIMED_OUT", timedOut.summary)
            timedOut
        } catch (ce: CancellationException) {
            throw ce
        } catch (ex: Exception) {
            val failed = ToolResult.Failure("'${step.toolName}' failed", ex.localizedMessage ?: "Unknown error")
            approvalManager.recordAudit(step.toolId, step.toolName, step.arguments, "FAILED", failed.reason)
            failed
        }
    }

    private fun buildFinalResponse(
        baseMessage: String,
        stepResults: List<Pair<AgentStep, ToolResult>>,
        warnings: List<String>
    ): String {
        val sb = StringBuilder(baseMessage.trim())
        if (stepResults.isNotEmpty()) {
            sb.append("\n\n")
            stepResults.forEach { (step, result) ->
                when (result) {
                    is ToolResult.Success -> {
                        sb.append("• ").append(result.summary)
                        if (result.details.isNotBlank()) {
                            sb.append("\n").append(result.details)
                        }
                        sb.append("\n")
                    }
                    is ToolResult.Failure -> {
                        sb.append("• ${step.toolName} failed: ${result.reason}\n")
                    }
                    is ToolResult.Rejected -> {
                        sb.append("• ${result.summary}\n")
                    }
                    is ToolResult.Cancelled -> {
                        sb.append("• ${result.summary}\n")
                    }
                    is ToolResult.TimedOut -> {
                        sb.append("• ${result.summary}\n")
                    }
                }
            }
        }
        if (warnings.isNotEmpty()) {
            sb.append("\nPolicy notes:\n")
            warnings.forEach { sb.append("• ").append(it).append("\n") }
        }
        return sb.toString().trim()
    }

    fun resetState() {
        _agentState.value = AgentState.Idle
    }
}
