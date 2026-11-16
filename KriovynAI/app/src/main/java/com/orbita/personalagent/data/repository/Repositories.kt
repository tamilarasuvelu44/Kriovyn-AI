package com.orbita.personalagent.data.repository

import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.data.local.db.ChatDao
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import com.orbita.personalagent.data.local.db.ChatSessionEntity
import com.orbita.personalagent.data.local.db.KriovynDatabase
import com.orbita.personalagent.data.local.db.MemoryDao
import com.orbita.personalagent.data.local.db.MessageRole
import com.orbita.personalagent.data.local.db.NoteDao
import com.orbita.personalagent.data.local.db.NoteEntity
import com.orbita.personalagent.data.local.db.TaskDao
import com.orbita.personalagent.data.local.db.TaskEntity
import com.orbita.personalagent.data.local.db.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    fun observeSessions(): Flow<List<ChatSessionEntity>> = chatDao.observeSessions()

    fun observeSession(sessionId: Long): Flow<ChatSessionEntity?> = chatDao.observeSession(sessionId)

    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.observeMessagesForSession(sessionId)

    suspend fun getMessagesSnapshot(sessionId: Long): List<ChatMessageEntity> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun getOrCreateActiveSession(): Long {
        val existing = chatDao.getMostRecentSession()
        return existing?.id ?: createSession("New Conversation")
    }

    suspend fun createSession(title: String = "New Conversation"): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertSession(
            ChatSessionEntity(
                title = title.ifBlank { "New Conversation" },
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameSession(sessionId: Long, newTitle: String) {
        val session = chatDao.getSessionById(sessionId) ?: return
        val cleaned = newTitle.trim().take(60)
        if (cleaned.isNotEmpty()) {
            chatDao.updateSession(
                session.copy(title = cleaned, updatedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun appendMessage(
        sessionId: Long,
        role: MessageRole,
        content: String,
        toolSummary: String? = null,
        isError: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val messageId = chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                content = content,
                toolSummary = toolSummary,
                isError = isError,
                timestamp = now
            )
        )
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val updatedTitle = if (
                session.title == "New Conversation" &&
                role == MessageRole.USER &&
                content.isNotBlank()
            ) {
                content.trim().replace("\n", " ").take(38)
            } else {
                session.title
            }
            chatDao.updateSession(
                session.copy(
                    title = updatedTitle,
                    lastMessagePreview = content.replace("\n", " ").take(90),
                    messageCount = session.messageCount + 1,
                    updatedAt = now
                )
            )
        }
        return messageId
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun clearAll() {
        chatDao.deleteAllSessions()
    }
}

class NoteRepository(private val noteDao: NoteDao) {
    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeNotes()

    suspend fun getRecentNotes(limit: Int = 20): List<NoteEntity> = noteDao.getRecentNotes(limit)

    suspend fun searchNotes(query: String): List<NoteEntity> = noteDao.searchNotes(query)

    suspend fun saveNote(
        title: String,
        content: String,
        tag: String = "General",
        isPinned: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        return noteDao.insertNote(
            NoteEntity(
                title = title.trim(),
                content = content.trim(),
                tag = tag.trim().ifBlank { "General" },
                isPinned = isPinned,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    suspend fun clearAll() {
        noteDao.deleteAllNotes()
    }
}

class TaskRepository(private val taskDao: TaskDao) {
    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeTasks()

    suspend fun getTasksSnapshot(completedFilter: Boolean? = null, limit: Int = 25): List<TaskEntity> {
        return when (completedFilter) {
            null -> taskDao.getAllTasksSnapshot(limit)
            else -> taskDao.getTasksByCompletion(completedFilter, limit)
        }
    }

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun createTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueAtMillis: Long? = null,
        reminderEnabled: Boolean = false,
        workRequestId: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        return taskDao.insertTask(
            TaskEntity(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                dueAtMillis = dueAtMillis,
                reminderEnabled = reminderEnabled,
                workRequestId = workRequestId,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAll() {
        taskDao.deleteAllTasks()
    }
}

class MemoryRepository(private val memoryDao: MemoryDao) {
    fun observeMemories(): Flow<List<ApprovedMemoryEntity>> = memoryDao.observeMemories()

    suspend fun getActiveMemories(): List<ApprovedMemoryEntity> = memoryDao.getActiveMemories()

    suspend fun saveApprovedMemory(
        category: String,
        keyLabel: String,
        content: String,
        sourceSessionId: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        return memoryDao.insertMemory(
            ApprovedMemoryEntity(
                category = category.trim().ifBlank { "Preference" },
                keyLabel = keyLabel.trim(),
                content = content.trim(),
                sourceSessionId = sourceSessionId,
                isActive = true,
                approvedAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateMemory(memory: ApprovedMemoryEntity) {
        memoryDao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(memory: ApprovedMemoryEntity) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearAll() {
        memoryDao.deleteAllMemories()
    }
}

class LocalDataManagementRepository(private val database: KriovynDatabase) {
    suspend fun clearAllDatabaseData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
