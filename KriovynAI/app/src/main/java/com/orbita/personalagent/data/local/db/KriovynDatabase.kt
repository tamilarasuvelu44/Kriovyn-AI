package com.orbita.personalagent.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        NoteEntity::class,
        TaskEntity::class,
        ApprovedMemoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(KriovynTypeConverters::class)
abstract class KriovynDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val DATABASE_NAME = "kriovyn_personal_agent.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tasks_is_completed ON tasks(is_completed)"
                )
            }
        }

        fun build(context: Context): KriovynDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KriovynDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
