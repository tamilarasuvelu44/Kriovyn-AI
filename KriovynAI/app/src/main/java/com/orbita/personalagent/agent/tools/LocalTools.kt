package com.orbita.personalagent.agent.tools

import com.orbita.personalagent.agent.AgentTool
import com.orbita.personalagent.agent.ArgumentValidation
import com.orbita.personalagent.agent.ToolResult
import com.orbita.personalagent.agent.ToolRiskLevel
import com.orbita.personalagent.data.local.db.TaskPriority
import com.orbita.personalagent.data.repository.NoteRepository
import com.orbita.personalagent.data.repository.TaskRepository

class SaveLocalNoteTool(
    private val noteRepository: NoteRepository
) : AgentTool {
    override val id = "save_local_note"
    override val displayName = "Save Local Note"
    override val description = "Save a note locally in Kriovyn Notes."
    override val riskLevel = ToolRiskLevel.LOW_LOCAL
    override val requiresApproval = false
    override val targetActionSummary = "Writes a new note to the local Room database."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val content = arguments["content"]?.trim().orEmpty()
        val title = arguments["title"]?.trim().orEmpty()
        if (content.isEmpty() && title.isEmpty()) {
            return ArgumentValidation.Invalid("Note title or content is required.")
        }
        if (content.length > 5_000) {
            return ArgumentValidation.Invalid("Note content exceeds 5,000 characters.")
        }
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val title = arguments["title"]?.trim().orEmpty().ifBlank { "Quick Note" }
        val content = arguments["content"]?.trim().orEmpty().ifBlank { title }
        val tag = arguments["tag"]?.trim().orEmpty().ifBlank { "General" }
        val noteId = noteRepository.saveNote(title = title, content = content, tag = tag)
        return ToolResult.Success(
            summary = "Saved note \"$title\" (#$noteId)",
            details = "Tag: $tag"
        )
    }
}

class CreateTaskTool(
    private val taskRepository: TaskRepository
) : AgentTool {
    override val id = "create_task"
    override val displayName = "Create Task"
    override val description = "Create a task in your local Kriovyn Tasks list."
    override val riskLevel = ToolRiskLevel.LOW_LOCAL
    override val requiresApproval = false
    override val targetActionSummary = "Adds a task to the local Room database."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val title = arguments["title"]?.trim().orEmpty()
        if (title.isEmpty()) return ArgumentValidation.Invalid("Task title is required.")
        if (title.length > 200) return ArgumentValidation.Invalid("Task title exceeds 200 characters.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val title = arguments["title"]!!.trim()
        val description = arguments["description"]?.trim().orEmpty()
        val priority = arguments["priority"]
            ?.let { runCatching { TaskPriority.valueOf(it.uppercase()) }.getOrNull() }
            ?: TaskPriority.MEDIUM
        val id = taskRepository.createTask(
            title = title,
            description = description,
            priority = priority
        )
        return ToolResult.Success(
            summary = "Created task \"$title\" (#$id)",
            details = "Priority: ${priority.label}"
        )
    }
}

class ListTasksTool(
    private val taskRepository: TaskRepository
) : AgentTool {
    override val id = "list_tasks"
    override val displayName = "List Tasks"
    override val description = "List local tasks from Kriovyn Tasks."
    override val riskLevel = ToolRiskLevel.LOW_LOCAL
    override val requiresApproval = false
    override val targetActionSummary = "Reads local tasks from Room."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val filter = arguments["filter"]?.lowercase() ?: "all"
        return if (filter in setOf("all", "pending", "completed")) {
            ArgumentValidation.Valid
        } else {
            ArgumentValidation.Invalid("Filter must be 'all', 'pending', or 'completed'.")
        }
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val filter = when (arguments["filter"]?.lowercase()) {
            "pending" -> false
            "completed" -> true
            else -> null
        }
        val tasks = taskRepository.getTasksSnapshot(completedFilter = filter, limit = 15)
        if (tasks.isEmpty()) {
            return ToolResult.Success(summary = "No matching tasks found.")
        }
        val formatted = tasks.joinToString("\n") { task ->
            val mark = if (task.isCompleted) "[✓]" else "[ ]"
            "$mark ${task.title} (${task.priority.label})"
        }
        return ToolResult.Success(
            summary = "Found ${tasks.size} task(s)",
            details = formatted
        )
    }
}

class ListNotesTool(
    private val noteRepository: NoteRepository
) : AgentTool {
    override val id = "list_notes"
    override val displayName = "List Notes"
    override val description = "List or search local notes."
    override val riskLevel = ToolRiskLevel.LOW_LOCAL
    override val requiresApproval = false
    override val targetActionSummary = "Reads local notes from Room."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]?.trim().orEmpty()
        val notes = if (query.isBlank()) {
            noteRepository.getRecentNotes(limit = 15)
        } else {
            noteRepository.searchNotes(query)
        }
        if (notes.isEmpty()) {
            return ToolResult.Success(summary = "No matching notes found.")
        }
        val formatted = notes.joinToString("\n") { "• ${it.title} [${it.tag}]" }
        return ToolResult.Success(
            summary = "Found ${notes.size} note(s)",
            details = formatted
        )
    }
}
