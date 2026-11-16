package com.orbita.personalagent.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.kriovynDataStore: DataStore<Preferences> by preferencesDataStore(name = "kriovyn_settings")

enum class KriovynThemeMode(val displayName: String) {
    DEEP_NAVY("Deep Navy (Default)"),
    OBSIDIAN_OLED("Pure Obsidian Black")
}

enum class AiBackendMode(val displayName: String) {
    DEMO_OFFLINE("Demo Mode (Offline)"),
    SECURE_BACKEND("Gemini Backend Adapter")
}

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: KriovynThemeMode = KriovynThemeMode.DEEP_NAVY,
    val aiBackendMode: AiBackendMode = AiBackendMode.DEMO_OFFLINE,
    val backendEndpointUrl: String = "",
    val memorySuggestionsEnabled: Boolean = true,
    val showToolStepDetails: Boolean = true
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AI_BACKEND_MODE = stringPreferencesKey("ai_backend_mode")
        val BACKEND_ENDPOINT_URL = stringPreferencesKey("backend_endpoint_url")
        val MEMORY_SUGGESTIONS_ENABLED = booleanPreferencesKey("memory_suggestions_enabled")
        val SHOW_TOOL_STEP_DETAILS = booleanPreferencesKey("show_tool_step_details")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserPreferences(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { KriovynThemeMode.valueOf(it) }.getOrNull() }
                    ?: KriovynThemeMode.DEEP_NAVY,
                aiBackendMode = prefs[Keys.AI_BACKEND_MODE]
                    ?.let { runCatching { AiBackendMode.valueOf(it) }.getOrNull() }
                    ?: AiBackendMode.DEMO_OFFLINE,
                backendEndpointUrl = prefs[Keys.BACKEND_ENDPOINT_URL] ?: "",
                memorySuggestionsEnabled = prefs[Keys.MEMORY_SUGGESTIONS_ENABLED] ?: true,
                showToolStepDetails = prefs[Keys.SHOW_TOOL_STEP_DETAILS] ?: true
            )
        }

    suspend fun getSnapshot(): UserPreferences = preferencesFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: KriovynThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAiBackendMode(mode: AiBackendMode) {
        dataStore.edit { it[Keys.AI_BACKEND_MODE] = mode.name }
    }

    suspend fun setBackendEndpointUrl(url: String) {
        dataStore.edit { it[Keys.BACKEND_ENDPOINT_URL] = url.trim() }
    }

    suspend fun setMemorySuggestionsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MEMORY_SUGGESTIONS_ENABLED] = enabled }
    }

    suspend fun setShowToolStepDetails(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_TOOL_STEP_DETAILS] = show }
    }

    suspend fun resetNonOnboardingPreferences() {
        dataStore.edit { prefs ->
            val onboarded = prefs[Keys.ONBOARDING_COMPLETED] ?: true
            prefs.clear()
            prefs[Keys.ONBOARDING_COMPLETED] = onboarded
        }
    }
}
