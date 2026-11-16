package com.orbita.personalagent.agent.tools

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.net.toUri
import com.orbita.personalagent.agent.AgentTool
import com.orbita.personalagent.agent.ArgumentValidation
import com.orbita.personalagent.agent.ToolResult
import com.orbita.personalagent.agent.ToolRiskLevel

class OpenWebSearchTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "open_web_search"
    override val displayName = "Open Web Search"
    override val description = "Open browser to search the web."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches ACTION_VIEW with an HTTPS search URL."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val query = arguments["query"]?.trim().orEmpty()
        if (query.isEmpty()) return ArgumentValidation.Invalid("Search query cannot be empty.")
        if (query.length > 250) return ArgumentValidation.Invalid("Search query is too long.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]!!.trim()
        val uri = "https://www.google.com/search?q=${Uri.encode(query)}".toUri()
        return when (val out = launcher.launch(Intent(Intent.ACTION_VIEW, uri))) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened web search for \"$query\"")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open web search", out.message)
        }
    }
}

class OpenMapSearchTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "open_map_search"
    override val displayName = "Open Map Search"
    override val description = "Open Maps to search a place or address."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches ACTION_VIEW with a geo:0,0?q= intent."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val query = arguments["query"]?.trim().orEmpty()
        if (query.isEmpty()) return ArgumentValidation.Invalid("Map query cannot be empty.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]!!.trim()
        val uri = "geo:0,0?q=${Uri.encode(query)}".toUri()
        return when (val out = launcher.launch(Intent(Intent.ACTION_VIEW, uri))) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened map search for \"$query\"")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open Maps", out.message)
        }
    }
}

class OpenDialerTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "open_dialer"
    override val displayName = "Open Phone Dialer"
    override val description = "Open the system dialer pre-filled with a phone number (never places a call automatically)."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches Intent.ACTION_DIAL (user must press Call)."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val phone = arguments["phoneNumber"]?.trim().orEmpty()
        if (!Regex("^[+\\d][\\d\\s()-]{3,20}$").matches(phone)) {
            return ArgumentValidation.Invalid("Valid phone number is required.")
        }
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val phone = arguments["phoneNumber"]!!.trim()
        val intent = Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(phone)}".toUri())
        return when (val out = launcher.launch(intent)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened dialer for $phone")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open dialer", out.message)
        }
    }
}

class PrepareSmsTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "prepare_sms"
    override val displayName = "Prepare SMS Draft"
    override val description = "Open your SMS app with recipient and draft message for review (never sends silently)."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches Intent.ACTION_SENDTO (smsto:) for user confirmation."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val phone = arguments["phoneNumber"]?.trim().orEmpty()
        val message = arguments["message"]?.trim().orEmpty()
        if (!Regex("^[+\\d][\\d\\s()-]{3,20}$").matches(phone)) {
            return ArgumentValidation.Invalid("Valid recipient phone number is required.")
        }
        if (message.isEmpty()) return ArgumentValidation.Invalid("SMS message draft cannot be empty.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val phone = arguments["phoneNumber"]!!.trim()
        val message = arguments["message"]!!.trim()
        val intent = Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(phone)}".toUri()).apply {
            putExtra("sms_body", message)
        }
        return when (val out = launcher.launch(intent)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Prepared SMS draft to $phone")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open SMS app", out.message)
        }
    }
}

class PrepareEmailTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "prepare_email"
    override val displayName = "Prepare Email Draft"
    override val description = "Open your email client with a pre-filled draft (never sends silently)."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches Intent.ACTION_SENDTO (mailto:) for user review."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val to = arguments["to"]?.trim().orEmpty()
        if (!Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(to)) {
            return ArgumentValidation.Invalid("Valid email address is required.")
        }
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val to = arguments["to"]!!.trim()
        val subject = arguments["subject"]?.trim().orEmpty()
        val body = arguments["body"]?.trim().orEmpty()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return when (val out = launcher.launch(intent)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Prepared email draft to $to")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open email client", out.message)
        }
    }
}

class OpenCalendarEventTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "open_calendar_event"
    override val displayName = "Open Calendar Event Form"
    override val description = "Open the system Calendar app with a pre-filled event form."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches CalendarContract INSERT intent."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val title = arguments["title"]?.trim().orEmpty()
        if (title.isEmpty()) return ArgumentValidation.Invalid("Event title is required.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val title = arguments["title"]!!.trim()
        val description = arguments["description"]?.trim().orEmpty()
        val beginMillis = arguments["beginMillis"]?.toLongOrNull()
            ?: (System.currentTimeMillis() + 3_600_000L)
        val endMillis = arguments["endMillis"]?.toLongOrNull()
            ?: (beginMillis + 3_600_000L)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
        return when (val out = launcher.launch(intent)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened calendar event form for \"$title\"")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open Calendar app", out.message)
        }
    }
}

class OpenClockAlarmTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "open_clock_alarm"
    override val displayName = "Open Clock Alarm Screen"
    override val description = "Open the Android Clock app to confirm setting an alarm."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches AlarmClock.ACTION_SET_ALARM with Clock UI visible."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val hour = arguments["hour"]?.toIntOrNull()
        val minute = arguments["minute"]?.toIntOrNull()
        if (hour == null || hour !in 0..23) {
            return ArgumentValidation.Invalid("Hour must be between 0 and 23.")
        }
        if (minute == null || minute !in 0..59) {
            return ArgumentValidation.Invalid("Minute must be between 0 and 59.")
        }
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val hour = arguments["hour"]!!.toInt()
        val minute = arguments["minute"]!!.toInt()
        val label = arguments["label"]?.trim().orEmpty().ifBlank { "Kriovyn AI Alarm" }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return when (val out = launcher.launch(intent)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened Clock alarm screen for %02d:%02d".format(hour, minute))
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open Clock app", out.message)
        }
    }
}

class ShareTextTool(
    private val launcher: AndroidIntentLauncher
) : AgentTool {
    override val id = "share_text"
    override val displayName = "Share Text"
    override val description = "Open the Android share sheet with text."
    override val riskLevel = ToolRiskLevel.EXTERNAL_INTENT
    override val requiresApproval = true
    override val targetActionSummary = "Launches Android Intent.createChooser(ACTION_SEND)."

    override fun validateArguments(arguments: Map<String, String>): ArgumentValidation {
        val text = arguments["text"]?.trim().orEmpty()
        if (text.isEmpty()) return ArgumentValidation.Invalid("Text to share cannot be empty.")
        return ArgumentValidation.Valid
    }

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val text = arguments["text"]!!.trim()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share via Kriovyn AI")
        return when (val out = launcher.launch(chooser)) {
            IntentLaunchOutcome.Launched ->
                ToolResult.Success("Opened Android share sheet")
            is IntentLaunchOutcome.NotSupported ->
                ToolResult.Failure("Could not open share sheet", out.message)
        }
    }
}
