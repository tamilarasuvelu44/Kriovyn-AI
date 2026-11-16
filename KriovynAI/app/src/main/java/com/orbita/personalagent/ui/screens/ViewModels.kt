package com.orbita.personalagent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.orbita.personalagent.agent.AgentOrchestrator
import com.orbita.personalagent.agent.AgentState
import com.orbita.personalagent.agent.ApprovalDecision
import com.orbita.personalagent.agent.ApprovalManager
import com.orbita.personalagent.agent.ToolApprovalRequest
import com.orbita.personalagent.agent.ToolAuditEntry
import com.orbita.personalagent.ai.AiProviderManager
import com.orbita.personalagent.ai.ConnectionCheckResult
import com.orbita.personalagent.ai.MemoryCandidate
import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import com.orbita.personalagent.data.local.db.ChatSessionEntity
import com.orbita.personalagent.data.local.db.NoteEntity
import com.orbita.personalagent.data.local.db.TaskEntity
import com.orbita.personalagent.data.local.db.TaskPriority
import com.orbita.personalagent.data.preferences.AiBackendMode
import com.orbita.personalagent.data.preferences.KriovynThemeMode
import com.orbita.personalagent.data.preferences.SettingsRepository
import com.orbita.personalagent.data.preferences.UserPreferences
import com.orbita.personalagent.data.repository.ChatRepository
import com.orbita.personalagent.data.repository.LocalDataManagementRepository
import com.orbita.personalagent.data.repository.MemoryRepository
import com.orbita.personalagent.data.repository.NoteRepository
import com.orbita.personalagent.data.repository.TaskRepository
import com.orbita.personalagent.di.AppContainer
import com.orbita.personalagent.worker.TaskReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    approvalManager: ApprovalManager
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val pendingApprovals: StateFlow<List<ToolApprovalRequest>> = approvalManager.pendingRequests

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val approvalManager: ApprovalManager,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    val activeSession: StateFlow<ChatSessionEntity?> = _activeSessionId
        .flatMapLatest { id -> if (id == null) flowOf(null) else chatRepository.observeSession(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<ChatMessageEntity>> = _activeSessionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else chatRepository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val agentState: StateFlow<AgentState> = agentOrchestrator.agentState
    val pendingApprovals: StateFlow<List<ToolApprovalRequest>> = approvalManager.pendingRequests
    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _memoryCandidates = MutableStateFlow<List<MemoryCandidate>>(emptyList())
    val memoryCandidates: StateFlow<List<MemoryCandidate>> = _memoryCandidates.asStateFlow()

    private var lastSubmittedPrompt: String? = null

    init {
        viewModelScope.launch {
            val initialSession = chatRepository.getOrCreateActiveSession()
            _activeSessionId.value = initialSession
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun openSession(sessionId: Long) {
        _activeSessionId.value = sessionId
        _memoryCandidates.value = emptyList()
        agentOrchestrator.resetState()
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val newId = chatRepository.createSession("New Conversation")
            _activeSessionId.value = newId
            _memoryCandidates.value = emptyList()
            agentOrchestrator.resetState()
        }
    }

    fun sendMessage(overridePrompt: String? = null) {
        val text = (overridePrompt ?: _inputText.value).trim()
        if (text.isEmpty()) return
        if (overridePrompt == null) {
            _inputText.value = ""
        }
        lastSubmittedPrompt = text

        viewModelScope.launch {
            val sessionId = _activeSessionId.value ?: chatRepository.getOrCreateActiveSession().also {
                _activeSessionId.value = it
            }
            val proposals = agentOrchestrator.processUserMessage(
                sessionId = sessionId,
                userInput = text,
                persistUserMessage = true
            )
            if (proposals.isNotEmpty()) {
                _memoryCandidates.update { current -> current + proposals }
            }
        }
    }

    fun retryLastMessage() {
        val prompt = lastSubmittedPrompt ?: return
        viewModelScope.launch {
            val sessionId = _activeSessionId.value ?: return@launch
            val proposals = agentOrchestrator.processUserMessage(
                sessionId = sessionId,
                userInput = prompt,
                persistUserMessage = false
            )
            if (proposals.isNotEmpty()) {
                _memoryCandidates.update { current -> current + proposals }
            }
        }
    }

    fun approveMemoryCandidate(candidate: MemoryCandidate) {
        viewModelScope.launch {
            memoryRepository.saveApprovedMemory(
                category = candidate.category,
                keyLabel = candidate.keyLabel,
                content = candidate.content,
                sourceSessionId = _activeSessionId.value
            )
            _memoryCandidates.update { list -> list.filterNot { it.id == candidate.id } }
        }
    }

    fun dismissMemoryCandidate(candidate: MemoryCandidate) {
        _memoryCandidates.update { list -> list.filterNot { it.id == candidate.id } }
    }
}

class HistoryViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = combine(
        chatRepository.observeSessions(),
        _searchQuery
    ) { allSessions, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) allSessions
        else allSessions.filter {
            it.title.lowercase().contains(q) || it.lastMessagePreview.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
        }
    }

    fun renameSession(sessionId: Long, title: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, title)
        }
    }

    fun createNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createSession("New Conversation")
            onCreated(id)
        }
    }
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = combine(
        taskRepository.observeTasks(),
        _filter
    ) { list, activeFilter ->
        when (activeFilter) {
            TaskFilter.ALL -> list
            TaskFilter.PENDING -> list.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> list.filter { it.isCompleted }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        reminderDelayMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val triggerAt = reminderDelayMinutes?.let {
                System.currentTimeMillis() + (it * 60_000L)
            }
            val taskId = taskRepository.createTask(
                title = title,
                description = description,
                priority = priority,
                dueAtMillis = triggerAt,
                reminderEnabled = triggerAt != null
            )
            if (triggerAt != null) {
                val workId = reminderScheduler.scheduleReminder(
                    taskId = taskId,
                    title = title.trim(),
                    description = description.trim(),
                    triggerAtMillis = triggerAt
                )
                taskRepository.getTaskById(taskId)?.let { created ->
                    taskRepository.updateTask(created.copy(workRequestId = workId))
                }
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            if (nowCompleted && task.reminderEnabled) {
                reminderScheduler.cancelReminder(task.id, task.workRequestId)
            }
            taskRepository.updateTask(
                task.copy(
                    isCompleted = nowCompleted,
                    reminderEnabled = if (nowCompleted) false else task.reminderEnabled
                )
            )
        }
    }

    fun updateTaskDetails(
        task: TaskEntity,
        newTitle: String,
        newDescription: String,
        newPriority: TaskPriority
    ) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateTask(
                task.copy(
                    title = newTitle.trim(),
                    description = newDescription.trim(),
                    priority = newPriority
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(task.id, task.workRequestId)
            taskRepository.deleteTask(task)
        }
    }
}

class NotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = combine(
        noteRepository.observeNotes(),
        _query
    ) { allNotes, q ->
        val trimmed = q.trim().lowercase()
        if (trimmed.isEmpty()) allNotes
        else allNotes.filter {
            it.title.lowercase().contains(trimmed) ||
                it.content.lowercase().contains(trimmed) ||
                it.tag.lowercase().contains(trimmed)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun saveNote(title: String, content: String, tag: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            noteRepository.saveNote(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                tag = tag.ifBlank { "General" }
            )
        }
    }

    fun updateNote(note: NoteEntity, title: String, content: String, tag: String, isPinned: Boolean) {
        viewModelScope.launch {
            noteRepository.updateNote(
                note.copy(
                    title = title.trim().ifBlank { "Untitled Note" },
                    content = content.trim(),
                    tag = tag.trim().ifBlank { "General" },
                    isPinned = isPinned
                )
            )
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}

class MemoryViewModel(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<ApprovedMemoryEntity>> = memoryRepository.observeMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMemory(category: String, keyLabel: String, content: String) {
        if (keyLabel.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            memoryRepository.saveApprovedMemory(
                category = category.ifBlank { "Preference" },
                keyLabel = keyLabel,
                content = content
            )
        }
    }

    fun updateMemory(memory: ApprovedMemoryEntity, category: String, keyLabel: String, content: String) {
        if (keyLabel.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            memoryRepository.updateMemory(
                memory.copy(
                    category = category.trim().ifBlank { "Preference" },
                    keyLabel = keyLabel.trim(),
                    content = content.trim()
                )
            )
        }
    }

    fun toggleMemoryActive(memory: ApprovedMemoryEntity) {
        viewModelScope.launch {
            memoryRepository.updateMemory(memory.copy(isActive = !memory.isActive))
        }
    }

    fun deleteMemory(memory: ApprovedMemoryEntity) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memory)
        }
    }
}

class ApprovalViewModel(
    private val approvalManager: ApprovalManager
) : ViewModel() {

    val pendingRequests: StateFlow<List<ToolApprovalRequest>> = approvalManager.pendingRequests
    val auditLog: StateFlow<List<ToolAuditEntry>> = approvalManager.auditLog

    fun approve(requestId: String) {
        approvalManager.resolve(requestId, ApprovalDecision.APPROVED)
    }

    fun reject(requestId: String) {
        approvalManager.resolve(requestId, ApprovalDecision.REJECTED)
    }

    fun clearAuditLog() {
        approvalManager.clearAuditLog()
    }
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val localDataManagementRepository: LocalDataManagementRepository,
    private val aiProviderManager: AiProviderManager,
    private val approvalManager: ApprovalManager
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _connectionCheck = MutableStateFlow<ConnectionCheckResult?>(null)
    val connectionCheck: StateFlow<ConnectionCheckResult?> = _connectionCheck.asStateFlow()

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        runConnectionDiagnostic()
    }

    fun setThemeMode(mode: KriovynThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setMemorySuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMemorySuggestionsEnabled(enabled) }
    }

    fun setShowToolStepDetails(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowToolStepDetails(show) }
    }

    fun setAiBackendMode(mode: AiBackendMode) {
        viewModelScope.launch {
            settingsRepository.setAiBackendMode(mode)
            runConnectionDiagnostic()
        }
    }

    fun saveBackendEndpointUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setBackendEndpointUrl(url)
            runConnectionDiagnostic()
        }
    }

    fun runConnectionDiagnostic() {
        viewModelScope.launch {
            _isCheckingConnection.value = true
            val provider = aiProviderManager.getActiveProvider()
            _connectionCheck.value = provider.checkConnection()
            _isCheckingConnection.value = false
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            approvalManager.cancelAllPending()
            approvalManager.clearAuditLog()
            localDataManagementRepository.clearAllDatabaseData()
            settingsRepository.resetNonOnboardingPreferences()
            _statusBanner.value = "All local chats, tasks, notes, and memories have been erased."
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }
}

class KriovynViewModelFactory(private val container: AppContainer) {
    val factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { MainViewModel(container.settingsRepository, container.approvalManager) }
        initializer {
            ChatViewModel(
                container.chatRepository,
                container.memoryRepository,
                container.agentOrchestrator,
                container.approvalManager,
                container.settingsRepository
            )
        }
        initializer { HistoryViewModel(container.chatRepository) }
        initializer { TasksViewModel(container.taskRepository, container.taskReminderScheduler) }
        initializer { NotesViewModel(container.noteRepository) }
        initializer { MemoryViewModel(container.memoryRepository) }
        initializer { ApprovalViewModel(container.approvalManager) }
        initializer {
            SettingsViewModel(
                container.settingsRepository,
                container.localDataManagementRepository,
                container.aiProviderManager,
                container.approvalManager
            )
        }
    }
}
