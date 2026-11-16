package com.orbita.personalagent.ai

interface AiProvider {
    val providerId: String
    val displayName: String
    val isDemoMode: Boolean

    suspend fun checkConnection(): ConnectionCheckResult
    suspend fun planAndRespond(request: AiRequest): AiResponse
}
