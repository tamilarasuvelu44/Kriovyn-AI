package com.orbita.personalagent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.orbita.personalagent.agent.AgentOrchestrator
import com.orbita.personalagent.agent.AgentTool
import com.orbita.personalagent.agent.ApprovalDecision
import com.orbita.personalagent.agent.ApprovalManager
import com.orbita.personalagent.agent.ArgumentValidation
import com.orbita.personalagent.agent.ToolPolicyEngine
import com.orbita.personalagent.agent.ToolRegistry
import com.orbita.personalagent.agent.ToolResult
import com.orbita.personalagent.agent.ToolRiskLevel
import com.orbita.personalagent.ai.AiProviderManager
import com.orbita.personalagent.ai.DemoAiProvider
import com.orbita.personalagent.ai.GeminiApiProvider
import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.data.local.db.ChatDao
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import com.orbita.personalagent.data.local.db.ChatSessionEntity
import com.orbita.personalagent.data.local.db.MemoryDao
import com.orbita.personalagent.data.preferences.SettingsRepository
import com.orbita.personalagent.data.repository.ChatRepository
import com.orbita.personalagent.data.repository.MemoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentOrchestratorTest {

    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    }

    private class FakeChatDao : ChatDao {
        private val sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
        private val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        private var nextSessionId = 1L
        private var nextMsgId = 1L

        override fun observeSessions() = sessions
        override fun observeSession(sessionId: Long) = sessions.map { list -> list.find { it.id == sessionId } }
        override suspend fun getSessionById(sessionId: Long) = sessions.value.find { it.id == sessionId }
        override suspend fun getMostRecentSession() = sessions.value.maxByOrNull { it.updatedAt }
        override suspend fun insertSession(session: ChatSessionEntity): Long {
            val id = if (session.id == 0L) nextSessionId++ else session.id
            sessions.value = sessions.value + session.copy(id = id)
            return id
        }
        override suspend fun updateSession(session: ChatSessionEntity) {
            sessions.value = sessions.value.map { if (it.id == session.id) session else it }
        }
        override suspend fun deleteSessionById(sessionId: Long) {
            sessions.value = sessions.value.filterNot { it.id == sessionId }
        }
        override fun observeMessagesForSession(sessionId: Long) =
            messages.map { list -> list.filter { it.sessionId == sessionId } }
        override suspend fun getMessagesForSession(sessionId: Long) =
            messages.value.filter { it.sessionId == sessionId }
        override suspend fun insertMessage(message: ChatMessageEntity): Long {
            val id = if (message.id == 0L) nextMsgId++ else message.id
            messages.value = messages.value + message.copy(id = id)
            return id
        }
        override suspend fun deleteMessageById(messageId: Long) {
            messages.value = messages.value.filterNot { it.id == messageId }
        }
        override suspend fun deleteAllSessions() {
            sessions.value = emptyList()
            messages.value = emptyList()
        }
    }

    private class FakeMemoryDao : MemoryDao {
        private val memories = MutableStateFlow<List<ApprovedMemoryEntity>>(emptyList())
        override fun observeMemories() = memories
        override suspend fun getActiveMemories() = memories.value.filter { it.isActive }
        override suspend fun insertMemory(memory: ApprovedMemoryEntity) = 1L
        override suspend fun updateMemory(memory: ApprovedMemoryEntity) {}
        override suspend fun deleteMemory(memory: ApprovedMemoryEntity) {}
        override suspend fun deleteAllMemories() { memories.value = emptyList() }
    }

    private fun buildOrchestrator(
        approvalManager: ApprovalManager,
        chatDao: FakeChatDao,
        tools: List<AgentTool>
    ): AgentOrchestrator {
        val settingsRepo = SettingsRepository(FakeDataStore())
        val registry = ToolRegistry(tools)
        val demo = DemoAiProvider()
        val aiManager = AiProviderManager(settingsRepo, demo, GeminiApiProvider({ "" }, registry))
        return AgentOrchestrator(
            aiProviderManager = aiManager,
            toolRegistry = registry,
            toolPolicyEngine = ToolPolicyEngine(registry),
            approvalManager = approvalManager,
            chatRepository = ChatRepository(chatDao),
            memoryRepository = MemoryRepository(FakeMemoryDao()),
            settingsRepository = settingsRepo
        )
    }

    @Test
    fun approvalExecutesExternalToolAndRejectionBlocksIt() = runTest {
        var executedCount = 0
        val webSearchStub = object : AgentTool {
            override val id = "open_web_search"
            override val displayName = "Open Web Search"
            override val description = "Search web"
            override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
            override val requiresApproval = true
            override val targetActionSummary = "Opens browser"
            override fun validateArguments(arguments: Map<String, String>) = ArgumentValidation.Valid
            override suspend fun execute(arguments: Map<String, String>): ToolResult {
                executedCount++
                return ToolResult.Success("Opened search")
            }
        }

        val approvalManager = ApprovalManager()
        val chatDao = FakeChatDao()
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        val orchestrator = buildOrchestrator(approvalManager, chatDao, listOf(webSearchStub))

        // 1. Test Rejection
        val rejectJob = async {
            orchestrator.processUserMessage(sessionId, "Search the web for Kotlin")
        }
        val firstPending = approvalManager.pendingRequests.first { it.isNotEmpty() }.first()
        approvalManager.resolve(firstPending.requestId, ApprovalDecision.REJECTED)
        rejectJob.await()
        assertEquals(0, executedCount)
        assertEquals("REJECTED", approvalManager.auditLog.value.first().status)

        // 2. Test Approval
        val approveJob = async {
            orchestrator.processUserMessage(sessionId, "Search the web for Compose")
        }
        val secondPending = approvalManager.pendingRequests.first { it.isNotEmpty() }.first()
        approvalManager.resolve(secondPending.requestId, ApprovalDecision.APPROVED)
        approveJob.await()
        assertEquals(1, executedCount)
        assertEquals("EXECUTED", approvalManager.auditLog.value.first().status)
    }

    @Test
    fun slowToolTimesOutCleanly() = runTest {
        val slowTool = object : AgentTool {
            override val id = "save_local_note"
            override val displayName = "Save Local Note"
            override val description = "Slow"
            override val riskLevel = ToolRiskLevel.LOW_LOCAL
            override val requiresApproval = false
            override val targetActionSummary = "Local"
            override fun validateArguments(arguments: Map<String, String>) = ArgumentValidation.Valid
            override suspend fun execute(arguments: Map<String, String>): ToolResult {
                delay(15_000L)
                return ToolResult.Success("Done")
            }
        }

        val approvalManager = ApprovalManager()
        val chatDao = FakeChatDao()
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Timeout"))
        val orchestrator = buildOrchestrator(approvalManager, chatDao, listOf(slowTool))

        orchestrator.processUserMessage(sessionId, "Save a note about timeout")
        assertTrue(approvalManager.auditLog.value.any { it.status == "TIMED_OUT" })
    }
}
