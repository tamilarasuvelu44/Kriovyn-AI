package com.orbita.personalagent.data.local.db

import androidx.room.TypeConverter

class KriovynTypeConverters {
    @TypeConverter
    fun fromMessageRole(role: MessageRole): String = role.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole =
        runCatching { MessageRole.valueOf(value) }.getOrDefault(MessageRole.ASSISTANT)

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority =
        runCatching { TaskPriority.valueOf(value) }.getOrDefault(TaskPriority.MEDIUM)
}
