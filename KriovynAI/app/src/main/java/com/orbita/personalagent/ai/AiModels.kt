package com.orbita.personalagent.ai

import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import kotlinx.serialization.Serializable

data class MemoryCandidate(
    val id: String,
    val category: String,
    val keyLabel: String,
    val content: String
)

data class ProposedToolCall(
    val toolId: String,
    val arguments: Map<String, String>,
    val rationale: String
)

data class AiRequest(
    val sessionId: Long,
    val userPrompt: String,
    val conversationHistory: List<ChatMessageEntity>,
    val approvedMemories: List<ApprovedMemoryEntity>,
    val memorySuggestionsEnabled: Boolean
)

data class AiResponse(
    val providerName: String,
    val isDemoMode: Boolean,
    val assistantMessage: String,
    val proposedToolCalls: List<ProposedToolCall> = emptyList(),
    val memoryCandidates: List<MemoryCandidate> = emptyList()
)

sealed interface ConnectionCheckResult {
    data class Ready(
        val providerName: String,
        val isDemoMode: Boolean,
        val detail: String,
        val latencyMs: Long
    ) : ConnectionCheckResult

    data class Unavailable(
        val providerName: String,
        val isDemoMode: Boolean,
        val reason: String
    ) : ConnectionCheckResult
}

@Serializable
data class BackendProxyRequestDto(
    val prompt: String,
    val memories: List<String>,
    val recentTurns: List<String>,
    val allowedTools: List<String>
)

@Serializable
data class BackendToolCallDto(
    val toolId: String,
    val arguments: Map<String, String> = emptyMap(),
    val rationale: String = ""
)

@Serializable
data class BackendMemoryCandidateDto(
    val category: String = "Preference",
    val keyLabel: String = "User Fact",
    val content: String
)

@Serializable
data class BackendProxyResponseDto(
    val reply: String,
    val toolCalls: List<BackendToolCallDto> = emptyList(),
    val memoryCandidates: List<BackendMemoryCandidateDto> = emptyList()
)
