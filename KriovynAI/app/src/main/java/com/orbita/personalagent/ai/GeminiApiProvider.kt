package com.orbita.personalagent.ai

import com.orbita.personalagent.agent.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Adapter for a future secure HTTPS backend proxy.
 * Never embeds or accepts raw Gemini API keys inside the Android APK.
 */
class GeminiApiProvider(
    private val endpointProvider: suspend () -> String,
    private val toolRegistry: ToolRegistry
) : AiProvider {

    override val providerId: String = "gemini_backend_adapter"
    override val displayName: String = "Gemini Secure Backend Adapter"
    override val isDemoMode: Boolean = false

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun checkConnection(): ConnectionCheckResult = withContext(Dispatchers.IO) {
        val endpoint = endpointProvider().trim()
        if (endpoint.isBlank()) {
            return@withContext ConnectionCheckResult.Unavailable(
                providerName = displayName,
                isDemoMode = false,
                reason = "No backend proxy URL configured. Switch to Demo Mode (Offline) or enter your HTTPS backend URL."
            )
        }
        if (!endpoint.startsWith("https://")) {
            return@withContext ConnectionCheckResult.Unavailable(
                providerName = displayName,
                isDemoMode = false,
                reason = "Security policy requires an HTTPS backend endpoint (no cleartext HTTP or APK API keys)."
            )
        }

        val start = System.currentTimeMillis()
        runCatching {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            try {
                val code = conn.responseCode
                val latency = System.currentTimeMillis() - start
                if (code in 200..405) {
                    ConnectionCheckResult.Ready(
                        providerName = displayName,
                        isDemoMode = false,
                        detail = "Backend endpoint reachable (HTTP $code).",
                        latencyMs = latency
                    )
                } else {
                    ConnectionCheckResult.Unavailable(
                        providerName = displayName,
                        isDemoMode = false,
                        reason = "Backend returned HTTP $code."
                    )
                }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { ex ->
            ConnectionCheckResult.Unavailable(
                providerName = displayName,
                isDemoMode = false,
                reason = ex.localizedMessage ?: "Unable to reach backend endpoint."
            )
        }
    }

    override suspend fun planAndRespond(request: AiRequest): AiResponse = withContext(Dispatchers.IO) {
        val endpoint = endpointProvider().trim()
        require(endpoint.startsWith("https://")) {
            "Secure Backend Adapter is not configured with an HTTPS URL. Switch to Demo Mode (Offline) in AI Connection Status."
        }

        val payload = BackendProxyRequestDto(
            prompt = request.userPrompt,
            memories = request.approvedMemories.map { "${it.category} - ${it.keyLabel}: ${it.content}" },
            recentTurns = request.conversationHistory.takeLast(8).map { "${it.role.name}: ${it.content}" },
            allowedTools = toolRegistry.allTools().map { it.id }
        )

        val bodyText = json.encodeToString(payload)
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(bodyText)
                writer.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val rawResponse = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException("Backend error (HTTP $code): ${rawResponse.take(180)}")
            }

            val dto = json.decodeFromString<BackendProxyResponseDto>(rawResponse)
            AiResponse(
                providerName = displayName,
                isDemoMode = false,
                assistantMessage = dto.reply,
                proposedToolCalls = dto.toolCalls.map {
                    ProposedToolCall(
                        toolId = it.toolId,
                        arguments = it.arguments,
                        rationale = it.rationale
                    )
                },
                memoryCandidates = if (request.memorySuggestionsEnabled) {
                    dto.memoryCandidates.mapIndexed { idx, candidate ->
                        MemoryCandidate(
                            id = "backend_mem_${System.currentTimeMillis()}_$idx",
                            category = candidate.category,
                            keyLabel = candidate.keyLabel,
                            content = candidate.content
                        )
                    }
                } else {
                    emptyList()
                }
            )
        } finally {
            conn.disconnect()
        }
    }
}
