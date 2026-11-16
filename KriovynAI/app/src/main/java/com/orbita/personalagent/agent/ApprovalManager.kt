package com.orbita.personalagent.agent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class ApprovalDecision {
    APPROVED,
    REJECTED,
    CANCELLED
}

data class ToolApprovalRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val sessionId: Long,
    val step: AgentStep,
    val targetActionSummary: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ToolAuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val status: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ApprovalManager {

    private val _pendingRequests = MutableStateFlow<List<ToolApprovalRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ToolApprovalRequest>> = _pendingRequests.asStateFlow()

    private val _auditLog = MutableStateFlow<List<ToolAuditEntry>>(emptyList())
    val auditLog: StateFlow<List<ToolAuditEntry>> = _auditLog.asStateFlow()

    private val waiters = mutableMapOf<String, CompletableDeferred<ApprovalDecision>>()

    suspend fun requestApproval(request: ToolApprovalRequest): ApprovalDecision {
        val deferred = CompletableDeferred<ApprovalDecision>()
        synchronized(waiters) {
            waiters[request.requestId] = deferred
        }
        _pendingRequests.update { it + request }
        return try {
            deferred.await()
        } finally {
            synchronized(waiters) {
                waiters.remove(request.requestId)
            }
            _pendingRequests.update { list -> list.filterNot { it.requestId == request.requestId } }
        }
    }

    fun resolve(requestId: String, decision: ApprovalDecision) {
        val deferred = synchronized(waiters) { waiters[requestId] }
        deferred?.complete(decision)
    }

    fun cancelAllPending() {
        val ids = synchronized(waiters) { waiters.keys.toList() }
        ids.forEach { resolve(it, ApprovalDecision.CANCELLED) }
    }

    fun recordAudit(
        toolId: String,
        toolName: String,
        arguments: Map<String, String>,
        status: String,
        detail: String
    ) {
        val entry = ToolAuditEntry(
            toolId = toolId,
            toolName = toolName,
            arguments = arguments,
            status = status,
            detail = detail
        )
        _auditLog.update { current -> (listOf(entry) + current).take(50) }
    }

    fun clearAuditLog() {
        _auditLog.value = emptyList()
    }
}
