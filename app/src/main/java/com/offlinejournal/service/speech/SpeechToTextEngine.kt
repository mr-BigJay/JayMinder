package com.offlinejournal.service.speech

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for offline speech-to-text engines.
 * Future online/AI engines can implement this interface without changing the UI layer.
 */
interface SpeechToTextEngine {
    val state: StateFlow<SpeechEngineState>

    suspend fun initialize(): Result<Unit>
    suspend fun transcribeFile(audioFilePath: String): Result<String>
    fun release()
}

sealed class SpeechEngineState {
    data object NotInitialized : SpeechEngineState()
    data object Initializing : SpeechEngineState()
    data class Ready(val modelName: String) : SpeechEngineState()
    data class Transcribing(val progress: Float = 0f) : SpeechEngineState()
    data class Error(val message: String) : SpeechEngineState()
}
