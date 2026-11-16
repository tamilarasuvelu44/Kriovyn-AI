package com.orbita.personalagent.agent.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface IntentLaunchOutcome {
    data object Launched : IntentLaunchOutcome
    data class NotSupported(val message: String) : IntentLaunchOutcome
}

class AndroidIntentLauncher(private val context: Context) {

    suspend fun launch(intent: Intent): IntentLaunchOutcome = withContext(Dispatchers.Main) {
        val safeIntent = intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(safeIntent)
            IntentLaunchOutcome.Launched
        } catch (_: ActivityNotFoundException) {
            IntentLaunchOutcome.NotSupported(
                "No compatible Android app is installed to handle this action."
            )
        } catch (se: SecurityException) {
            IntentLaunchOutcome.NotSupported(
                "Android security policy blocked this intent: ${se.localizedMessage}"
            )
        }
    }
}
