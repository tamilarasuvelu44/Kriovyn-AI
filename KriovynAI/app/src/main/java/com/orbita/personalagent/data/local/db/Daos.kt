package com.orbita.personalagent.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updated_at DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions ORDER BY updated_at DESC LIMIT 1")
    suspend fun getMostRecentSession(): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC LIMIT :limit")
    suspend fun getRecentNotes(limit: Int = 20): List<NoteEntity>

    @Query(
        """
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR tag LIKE '%' || :query || '%'
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, due_at_millis IS NULL ASC, due_at_millis ASC, updated_at DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_completed = :completed ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getTasksByCompletion(completed: Boolean, limit: Int = 25): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, updated_at DESC LIMIT :limit")
    suspend fun getAllTasksSnapshot(limit: Int = 25): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM approved_memories ORDER BY updated_at DESC")
    fun observeMemories(): Flow<List<ApprovedMemoryEntity>>

    @Query("SELECT * FROM approved_memories WHERE is_active = 1 ORDER BY updated_at DESC")
    suspend fun getActiveMemories(): List<ApprovedMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: ApprovedMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: ApprovedMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: ApprovedMemoryEntity)

    @Query("DELETE FROM approved_memories")
    suspend fun deleteAllMemories()
}
