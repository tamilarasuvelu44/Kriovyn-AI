package com.orbita.personalagent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import com.orbita.personalagent.data.local.db.ChatSessionEntity
import com.orbita.personalagent.data.local.db.KriovynDatabase
import com.orbita.personalagent.data.local.db.MessageRole
import com.orbita.personalagent.data.local.db.NoteEntity
import com.orbita.personalagent.data.local.db.TaskEntity
import com.orbita.personalagent.data.local.db.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KriovynDatabaseTest {

    private lateinit var db: KriovynDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KriovynDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun cascadeDeleteSessionRemovesMessages() = runTest {
        val sessionId = db.chatDao().insertSession(ChatSessionEntity(title = "Test Chat"))
        db.chatDao().insertMessage(
            ChatMessageEntity(sessionId = sessionId, role = MessageRole.USER, content = "Hello")
        )
        assertEquals(1, db.chatDao().getMessagesForSession(sessionId).size)

        db.chatDao().deleteSessionById(sessionId)
        assertTrue(db.chatDao().getMessagesForSession(sessionId).isEmpty())
    }

    @Test
    fun notesTasksAndMemoriesPersistCorrectly() = runTest {
        db.noteDao().insertNote(NoteEntity(title = "Architecture", content = "Room + Compose", isPinned = true))
        db.taskDao().insertTask(TaskEntity(title = "Verify build", priority = TaskPriority.HIGH))
        db.memoryDao().insertMemory(
            ApprovedMemoryEntity(category = "Preference", keyLabel = "Theme", content = "Deep Navy")
        )

        assertEquals(1, db.noteDao().observeNotes().first().size)
        assertEquals(1, db.taskDao().observeTasks().first().size)
        assertEquals(1, db.memoryDao().getActiveMemories().size)
    }
}
