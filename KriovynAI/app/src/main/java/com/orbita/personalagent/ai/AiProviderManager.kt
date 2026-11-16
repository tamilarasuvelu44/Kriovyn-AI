package com.orbita.personalagent.ai

import com.orbita.personalagent.data.preferences.AiBackendMode
import com.orbita.personalagent.data.preferences.SettingsRepository

class AiProviderManager(
    private val settingsRepository: SettingsRepository,
    val demoAiProvider: DemoAiProvider,
    val geminiApiProvider: GeminiApiProvider
) {
    suspend fun getActiveProvider(): AiProvider {
        val prefs = settingsRepository.getSnapshot()
        return when (prefs.aiBackendMode) {
            AiBackendMode.DEMO_OFFLINE -> demoAiProvider
            AiBackendMode.SECURE_BACKEND -> geminiApiProvider
        }
    }
}
