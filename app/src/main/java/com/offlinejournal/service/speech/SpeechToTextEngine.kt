package com.offlinejournal.service.speech

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for offline speech-to-text engines.
 * Future online/AI engines can implement this interface without changing the UI layer.
 */
interface SpeechToTextEngine {
    val state: StateFlow<SpeechEngineState>
    val modelState: StateFlow<ModelInstallState>

    suspend fun ensureModelInstalled(allowDownload: Boolean = true): Result<Unit>
    suspend fun initialize(): Result<Unit>
    suspend fun transcribeFile(audioFilePath: String): Result<String>

    /** Begin live recognition while recording PCM 16kHz mono chunks. */
    suspend fun startLiveRecognition(): Result<Unit>

    /** Feed PCM bytes captured during recording. */
    fun acceptPcmChunk(pcmChunk: ByteArray)

    /** Finish live session and return the best transcript. */
    suspend fun finishLiveRecognition(): Result<String>

    fun cancelLiveRecognition()
    fun release()
}

sealed class SpeechEngineState {
    data object NotInitialized : SpeechEngineState()
    data object Initializing : SpeechEngineState()
    data class Ready(val modelName: String) : SpeechEngineState()
    data class Transcribing(val progress: Float = 0f) : SpeechEngineState()
    data class LivePartial(val text: String) : SpeechEngineState()
    data class Error(val message: String) : SpeechEngineState()
}
