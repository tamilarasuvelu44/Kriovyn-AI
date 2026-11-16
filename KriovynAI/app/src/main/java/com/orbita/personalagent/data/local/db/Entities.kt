package com.orbita.personalagent.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageRole {
    USER,
    ASSISTANT,
    TOOL,
    ERROR
}

enum class TaskPriority(val label: String, val sortOrder: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2)
}

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String = "",
    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    val role: MessageRole,
    val content: String,
    @ColumnInfo(name = "tool_summary")
    val toolSummary: String? = null,
    @ColumnInfo(name = "is_error")
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val tag: String = "General",
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["is_completed"], name = "index_tasks_is_completed")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    @ColumnInfo(name = "due_at_millis")
    val dueAtMillis: Long? = null,
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,
    @ColumnInfo(name = "work_request_id")
    val workRequestId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "approved_memories")
data class ApprovedMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val category: String,
    @ColumnInfo(name = "key_label")
    val keyLabel: String,
    val content: String,
    @ColumnInfo(name = "source_session_id")
    val sourceSessionId: Long? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "approved_at")
    val approvedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
