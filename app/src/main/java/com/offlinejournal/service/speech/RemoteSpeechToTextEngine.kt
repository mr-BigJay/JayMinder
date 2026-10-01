package com.offlinejournal.service.speech

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Placeholder for a future cloud STT provider (e.g. Whisper API).
 * JayMinder currently uses [VoskSpeechToTextEngine] for raw transcription.
 */
class RemoteSpeechToTextEngine : SpeechToTextEngine {

    private val _state = MutableStateFlow<SpeechEngineState>(
        SpeechEngineState.Error("موتور STT ابری هنوز پیکربندی نشده است")
    )
    override val state: StateFlow<SpeechEngineState> = _state.asStateFlow()

    private val _modelState = MutableStateFlow<ModelInstallState>(ModelInstallState.NotInstalled)
    override val modelState: StateFlow<ModelInstallState> = _modelState.asStateFlow()

    private fun notAvailable(): Result<Unit> =
        Result.failure(UnsupportedOperationException("RemoteSpeechToTextEngine فعال نیست"))

    override suspend fun ensureModelInstalled(): Result<Unit> = notAvailable()

    override suspend fun initialize(): Result<Unit> = notAvailable()

    override suspend fun transcribeFile(audioFilePath: String): Result<String> =
        Result.failure(UnsupportedOperationException("RemoteSpeechToTextEngine فعال نیست"))

    override suspend fun startLiveRecognition(): Result<Unit> = notAvailable()

    override fun acceptPcmChunk(pcmChunk: ByteArray) = Unit

    override suspend fun finishLiveRecognition(): Result<String> =
        Result.failure(UnsupportedOperationException("RemoteSpeechToTextEngine فعال نیست"))

    override fun cancelLiveRecognition() = Unit

    override fun release() = Unit
}
