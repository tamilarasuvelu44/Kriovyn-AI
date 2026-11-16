package com.orbita.personalagent.ai

import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Offline deterministic AI provider for Phase 1 testing, demos, and privacy-first usage.
 * Recognizes natural user requests for all 12 Phase 1 allowlisted tools and proposes
 * explicit user-approved memories without silently persisting anything.
 */
class DemoAiProvider : AiProvider {
    override val providerId: String = "demo_offline"
    override val displayName: String = "Kriovyn Demo Engine (Offline)"
    override val isDemoMode: Boolean = true

    override suspend fun checkConnection(): ConnectionCheckResult {
        return ConnectionCheckResult.Ready(
            providerName = displayName,
            isDemoMode = true,
            detail = "Offline deterministic planner active. Zero network calls or API keys required.",
            latencyMs = 12L
        )
    }

    override suspend fun planAndRespond(request: AiRequest): AiResponse {
        delay(300L)
        val prompt = request.userPrompt.trim()
        val lower = prompt.lowercase()

        val calls = mutableListOf<ProposedToolCall>()
        val memoryCandidates = mutableListOf<MemoryCandidate>()

        if (request.memorySuggestionsEnabled) {
            extractMemoryCandidate(prompt, lower)?.let { memoryCandidates.add(it) }
        }

        val segments = splitCompoundSegments(prompt)
        for (segment in segments) {
            if (calls.size >= 5) break
            parseSingleSegment(segment)?.let { calls.add(it) }
        }

        if (calls.isEmpty()) {
            parseSingleSegment(prompt)?.let { calls.add(it) }
        }

        val reply = buildAssistantDraft(prompt, lower, calls, request, memoryCandidates)

        return AiResponse(
            providerName = displayName,
            isDemoMode = true,
            assistantMessage = reply,
            proposedToolCalls = calls,
            memoryCandidates = memoryCandidates
        )
    }

    private fun splitCompoundSegments(prompt: String): List<String> {
        val parts = prompt.split(Regex("\\s+(?:and then|then|and also|also|and)\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (parts.size > 1) parts else listOf(prompt)
    }

    private fun parseSingleSegment(segment: String): ProposedToolCall? {
        val lower = segment.lowercase()

        // List tasks
        if (lower.contains("list tasks") || lower.contains("show tasks") ||
            lower.contains("my tasks") || lower.contains("pending tasks") ||
            lower.contains("what are my tasks")
        ) {
            val filter = when {
                lower.contains("completed") || lower.contains("done") -> "completed"
                lower.contains("pending") || lower.contains("open") -> "pending"
                else -> "all"
            }
            return ProposedToolCall(
                toolId = "list_tasks",
                arguments = mapOf("filter" to filter),
                rationale = "Fetch your local tasks from Kriovyn database."
            )
        }

        // List notes
        if (lower.contains("list notes") || lower.contains("show notes") ||
            lower.contains("my notes") || lower.contains("find note")
        ) {
            val query = extractAfter(segment, listOf("about", "for", "matching")).orEmpty()
            return ProposedToolCall(
                toolId = "list_notes",
                arguments = mapOf("query" to query),
                rationale = "Retrieve your saved local notes."
            )
        }

        // Create task
        if (lower.startsWith("create task") || lower.startsWith("add task") ||
            lower.contains("create a task") || lower.contains("add a task") ||
            lower.startsWith("remind me to") || lower.startsWith("todo:")
        ) {
            val title = cleanPrefix(
                segment,
                listOf(
                    "create a task to",
                    "create task to",
                    "create a task",
                    "create task",
                    "add a task to",
                    "add task to",
                    "add task",
                    "remind me to",
                    "todo:"
                )
            ).ifBlank { "Follow-up task" }
            val priority = when {
                lower.contains("high priority") || lower.contains("urgent") -> "HIGH"
                lower.contains("low priority") -> "LOW"
                else -> "MEDIUM"
            }
            return ProposedToolCall(
                toolId = "create_task",
                arguments = mapOf(
                    "title" to title,
                    "description" to "Created via Kriovyn AI assistant",
                    "priority" to priority
                ),
                rationale = "Save a new task in your local Kriovyn task list."
            )
        }

        // Save local note
        if (lower.contains("save a note") || lower.contains("save note") ||
            lower.contains("take a note") || lower.startsWith("note:")
        ) {
            val body = cleanPrefix(
                segment,
                listOf(
                    "save a note about",
                    "save a note that",
                    "save a note",
                    "save note about",
                    "save note",
                    "take a note about",
                    "take a note",
                    "note:"
                )
            ).ifBlank { segment }
            val title = body.split(" ").take(5).joinToString(" ").ifBlank { "Quick Note" }
            return ProposedToolCall(
                toolId = "save_local_note",
                arguments = mapOf(
                    "title" to title,
                    "content" to body,
                    "tag" to "Assistant"
                ),
                rationale = "Store this note locally in your Kriovyn Notes vault."
            )
        }

        // Web search
        if (lower.contains("search the web") || lower.contains("web search") ||
            lower.startsWith("search for") || lower.startsWith("google ")
        ) {
            val query = cleanPrefix(
                segment,
                listOf("search the web for", "web search for", "web search", "search for", "google")
            ).ifBlank { segment }
            return ProposedToolCall(
                toolId = "open_web_search",
                arguments = mapOf("query" to query),
                rationale = "Open your browser to search the web for \"$query\"."
            )
        }

        // Map search
        if (lower.contains("on maps") || lower.contains("map search") ||
            lower.contains("directions to") || lower.contains("find near me") ||
            lower.contains("locate ")
        ) {
            val query = cleanPrefix(
                segment,
                listOf("search maps for", "map search for", "find directions to", "directions to", "locate", "find")
            ).replace(Regex("\\s+on maps$", RegexOption.IGNORE_CASE), "").trim().ifBlank { segment }
            return ProposedToolCall(
                toolId = "open_map_search",
                arguments = mapOf("query" to query),
                rationale = "Open Maps to search for \"$query\"."
            )
        }

        // Open dialer
        if (lower.startsWith("call ") || lower.contains("dial ") || lower.contains("open dialer")) {
            val phone = Regex("[+\\d][\\d\\s()-]{4,}").find(segment)?.value?.trim().orEmpty()
            if (phone.isNotEmpty()) {
                return ProposedToolCall(
                    toolId = "open_dialer",
                    arguments = mapOf("phoneNumber" to phone),
                    rationale = "Open the Android Dialer pre-filled with $phone (never calls automatically)."
                )
            }
        }

        // Prepare SMS
        if (lower.contains("sms") || lower.startsWith("text ") || lower.contains("send a text")) {
            val phone = Regex("[+\\d][\\d\\s()-]{4,}").find(segment)?.value?.trim().orEmpty()
            val message = extractAfter(segment, listOf("saying", "message", "that", ":")).orEmpty()
                .ifBlank { "Hi, checking in!" }
            if (phone.isNotEmpty()) {
                return ProposedToolCall(
                    toolId = "prepare_sms",
                    arguments = mapOf("phoneNumber" to phone, "message" to message),
                    rationale = "Open your Messages app with a draft SMS to $phone for your review."
                )
            }
        }

        // Prepare Email
        if (lower.contains("email") || lower.contains("mail to")) {
            val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(segment)?.value.orEmpty()
            val subject = extractAfter(segment, listOf("about", "subject")).orEmpty()
                .ifBlank { "Message from Kriovyn AI" }
            val body = extractAfter(segment, listOf("saying", "body", "with message")).orEmpty()
                .ifBlank { "Hello,\n\nDrafted with Kriovyn AI." }
            if (email.isNotEmpty()) {
                return ProposedToolCall(
                    toolId = "prepare_email",
                    arguments = mapOf("to" to email, "subject" to subject, "body" to body),
                    rationale = "Open your email client with a draft to $email (never sends silently)."
                )
            }
        }

        // Calendar event
        if (lower.contains("calendar") || lower.contains("schedule a meeting") || lower.contains("create event")) {
            val title = cleanPrefix(
                segment,
                listOf(
                    "create calendar event for",
                    "open calendar for",
                    "schedule a meeting for",
                    "schedule a meeting",
                    "create event"
                )
            ).ifBlank { "Team Sync" }
            return ProposedToolCall(
                toolId = "open_calendar_event",
                arguments = mapOf(
                    "title" to title,
                    "description" to "Scheduled via Kriovyn AI"
                ),
                rationale = "Open your Calendar app event form pre-filled with \"$title\"."
            )
        }

        // Clock alarm
        if (lower.contains("alarm")) {
            val timeMatch = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(segment)
            var hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 7
            val minute = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 30
            val amPm = timeMatch?.groupValues?.get(3)?.lowercase().orEmpty()
            if (amPm == "pm" && hour in 1..11) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            hour = hour.coerceIn(0, 23)
            val validMin = minute.coerceIn(0, 59)
            return ProposedToolCall(
                toolId = "open_clock_alarm",
                arguments = mapOf(
                    "hour" to hour.toString(),
                    "minute" to validMin.toString(),
                    "label" to "Kriovyn Alarm"
                ),
                rationale = "Open the system Clock alarm screen for %02d:%02d.".format(hour, validMin)
            )
        }

        // Share text
        if (lower.startsWith("share ") || lower.contains("share text")) {
            val text = cleanPrefix(segment, listOf("share text:", "share text", "share:")).ifBlank { segment }
            return ProposedToolCall(
                toolId = "share_text",
                arguments = mapOf("text" to text),
                rationale = "Open the Android Share sheet with your selected text."
            )
        }

        return null
    }

    private fun extractMemoryCandidate(prompt: String, lower: String): MemoryCandidate? {
        val prefixes = listOf("remember that ", "remember ", "my preference is ", "note my preference ")
        val matched = prefixes.firstOrNull { lower.startsWith(it) } ?: return null
        val fact = prompt.drop(matched.length).trim()
        if (fact.length < 4) return null
        return MemoryCandidate(
            id = UUID.randomUUID().toString(),
            category = if (lower.contains("prefer")) "Preference" else "Personal Context",
            keyLabel = fact.split(" ").take(4).joinToString(" "),
            content = fact
        )
    }

    private fun buildAssistantDraft(
        prompt: String,
        lower: String,
        calls: List<ProposedToolCall>,
        request: AiRequest,
        memoryCandidates: List<MemoryCandidate>
    ): String {
        if (request.approvedMemories.isNotEmpty() && lower.contains("what do you know")) {
            val list = request.approvedMemories.joinToString("\n") { "• ${it.keyLabel}: ${it.content}" }
            return "[Demo Mode] Here are your currently approved personal memories:\n$list"
        }

        if (calls.isNotEmpty()) {
            return "[Demo Mode] I've prepared ${calls.size} controlled action(s) for your request."
        }

        if (memoryCandidates.isNotEmpty()) {
            return "[Demo Mode] I won't save anything silently. Review the suggested memory card below if you'd like me to remember that."
        }

        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "[Demo Mode] Hello! I'm Kriovyn AI running offline in Demo Mode. Ask me to create tasks, save notes, list items, or safely prepare Android actions (web/map search, dialer, SMS, email, calendar, alarm, or share) with your approval."

            lower.contains("help") || lower.contains("what can you do") ->
                "[Demo Mode] In Phase 1, I can:\n" +
                    "• Organize Tasks & Notes locally in Room\n" +
                    "• Suggest Personal Memories (only saved when you approve)\n" +
                    "• Safely launch Web Search, Maps, Dialer, SMS, Email, Calendar, Alarm, and Share intents after explicit Tool Approval."

            else ->
                "[Demo Mode] I understood: \"$prompt\". To trigger a tool in Demo Mode, try phrasing like \"Create a task to review architecture\", \"Save a note about sprint goals\", \"Set alarm for 7:30 am\", or \"Prepare email to alex@example.com about launch\"."
        }
    }

    private fun cleanPrefix(text: String, prefixes: List<String>): String {
        val lower = text.lowercase()
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                return text.drop(prefix.length).trim()
            }
            val idx = lower.indexOf(prefix)
            if (idx >= 0) {
                return text.substring(idx + prefix.length).trim()
            }
        }
        return text.trim()
    }

    private fun extractAfter(text: String, markers: List<String>): String? {
        val lower = text.lowercase()
        for (marker in markers) {
            val idx = lower.indexOf(marker)
            if (idx >= 0) {
                val result = text.substring(idx + marker.length).trim()
                if (result.isNotEmpty()) return result
            }
        }
        return null
    }
}
