package com.orbita.personalagent.di

import android.content.Context
import com.orbita.personalagent.agent.AgentOrchestrator
import com.orbita.personalagent.agent.ApprovalManager
import com.orbita.personalagent.agent.ToolPolicyEngine
import com.orbita.personalagent.agent.ToolRegistry
import com.orbita.personalagent.agent.tools.AndroidIntentLauncher
import com.orbita.personalagent.agent.tools.CreateTaskTool
import com.orbita.personalagent.agent.tools.ListNotesTool
import com.orbita.personalagent.agent.tools.ListTasksTool
import com.orbita.personalagent.agent.tools.OpenCalendarEventTool
import com.orbita.personalagent.agent.tools.OpenClockAlarmTool
import com.orbita.personalagent.agent.tools.OpenDialerTool
import com.orbita.personalagent.agent.tools.OpenMapSearchTool
import com.orbita.personalagent.agent.tools.OpenWebSearchTool
import com.orbita.personalagent.agent.tools.PrepareEmailTool
import com.orbita.personalagent.agent.tools.PrepareSmsTool
import com.orbita.personalagent.agent.tools.SaveLocalNoteTool
import com.orbita.personalagent.agent.tools.ShareTextTool
import com.orbita.personalagent.ai.AiProviderManager
import com.orbita.personalagent.ai.DemoAiProvider
import com.orbita.personalagent.ai.GeminiApiProvider
import com.orbita.personalagent.data.local.db.KriovynDatabase
import com.orbita.personalagent.data.preferences.SettingsRepository
import com.orbita.personalagent.data.preferences.kriovynDataStore
import com.orbita.personalagent.data.repository.ChatRepository
import com.orbita.personalagent.data.repository.LocalDataManagementRepository
import com.orbita.personalagent.data.repository.MemoryRepository
import com.orbita.personalagent.data.repository.NoteRepository
import com.orbita.personalagent.data.repository.TaskRepository
import com.orbita.personalagent.worker.TaskReminderScheduler

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: KriovynDatabase by lazy { KriovynDatabase.build(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.kriovynDataStore) }

    val chatRepository: ChatRepository by lazy { ChatRepository(database.chatDao()) }
    val noteRepository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val localDataManagementRepository: LocalDataManagementRepository by lazy {
        LocalDataManagementRepository(database)
    }

    val taskReminderScheduler: TaskReminderScheduler by lazy { TaskReminderScheduler(appContext) }
    val intentLauncher: AndroidIntentLauncher by lazy { AndroidIntentLauncher(appContext) }

    val toolRegistry: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                SaveLocalNoteTool(noteRepository),
                CreateTaskTool(taskRepository),
                ListTasksTool(taskRepository),
                ListNotesTool(noteRepository),
                OpenWebSearchTool(intentLauncher),
                OpenMapSearchTool(intentLauncher),
                OpenDialerTool(intentLauncher),
                PrepareSmsTool(intentLauncher),
                PrepareEmailTool(intentLauncher),
                OpenCalendarEventTool(intentLauncher),
                OpenClockAlarmTool(intentLauncher),
                ShareTextTool(intentLauncher)
            )
        )
    }

    val toolPolicyEngine: ToolPolicyEngine by lazy { ToolPolicyEngine(toolRegistry) }
    val approvalManager: ApprovalManager by lazy { ApprovalManager() }

    val demoAiProvider: DemoAiProvider by lazy { DemoAiProvider() }
    val geminiApiProvider: GeminiApiProvider by lazy {
        GeminiApiProvider(
            endpointProvider = { settingsRepository.getSnapshot().backendEndpointUrl },
            toolRegistry = toolRegistry
        )
    }
    val aiProviderManager: AiProviderManager by lazy {
        AiProviderManager(settingsRepository, demoAiProvider, geminiApiProvider)
    }

    val agentOrchestrator: AgentOrchestrator by lazy {
        AgentOrchestrator(
            aiProviderManager = aiProviderManager,
            toolRegistry = toolRegistry,
            toolPolicyEngine = toolPolicyEngine,
            approvalManager = approvalManager,
            chatRepository = chatRepository,
            memoryRepository = memoryRepository,
            settingsRepository = settingsRepository
        )
    }
}
