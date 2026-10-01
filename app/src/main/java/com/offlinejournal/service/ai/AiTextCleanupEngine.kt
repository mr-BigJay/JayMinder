package com.offlinejournal.service.ai

/**
 * Post-processes raw offline STT output (e.g. Vosk) into cleaner Persian text.
 * Implementations may call a remote LLM or a future backend proxy.
 */
interface AiTextCleanupEngine {
    suspend fun cleanText(rawText: String): Result<String>
}
