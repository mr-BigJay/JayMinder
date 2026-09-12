package com.offlinejournal.service.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import com.offlinejournal.service.audio.WavFileUtils
import com.offlinejournal.service.audio.PcmAudioFormat
import java.io.File

/**
 * Offline Persian speech recognition using Vosk with optimized PCM/WAV pipeline.
 */
class VoskSpeechToTextEngine(
    private val context: Context,
    private val modelManager: VoskModelManager = VoskModelManager(context)
) : SpeechToTextEngine {

    private val _state = MutableStateFlow<SpeechEngineState>(SpeechEngineState.NotInitialized)
    override val state: StateFlow<SpeechEngineState> = _state.asStateFlow()

    override val modelState: StateFlow<ModelInstallState> = modelManager.state

    private var model: Model? = null
    private var liveRecognizer: Recognizer? = null
    private val partialTexts = mutableListOf<String>()

    override suspend fun ensureModelInstalled(): Result<Unit> {
        return modelManager.ensureModelInstalled().map { Unit }
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (model != null) {
            _state.value = SpeechEngineState.Ready(VoskModelManager.DEFAULT_MODEL_ID)
            return@withContext Result.success(Unit)
        }

        _state.value = SpeechEngineState.Initializing
        try {
            val modelDir = modelManager.ensureModelInstalled().getOrElse {
                return@withContext Result.failure(it)
            }
            model = Model(modelDir.absolutePath)
            _state.value = SpeechEngineState.Ready(VoskModelManager.DEFAULT_MODEL_ID)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = SpeechEngineState.Error(
                e.message ?: "خطا در بارگذاری مدل تشخیص گفتار"
            )
            Result.failure(e)
        }
    }

    override suspend fun transcribeFile(audioFilePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            val currentModel = loadModelOrFail().getOrElse { return@withContext Result.failure(it) }

            _state.value = SpeechEngineState.Transcribing(0f)
            try {
                val pcmData = readAudioAsPcm16(audioFilePath)
                if (pcmData.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("فایل صوتی خالی یا نامعتبر است"))
                }

                val text = recognizePcm(currentModel, pcmData)
                _state.value = SpeechEngineState.Ready(VoskModelManager.DEFAULT_MODEL_ID)
                Result.success(text)
            } catch (e: Exception) {
                _state.value = SpeechEngineState.Error("خطا در تبدیل صدا به متن: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun startLiveRecognition(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentModel = loadModelOrFail().getOrElse { return@withContext Result.failure(it) }
        cancelLiveRecognition()
        partialTexts.clear()
        liveRecognizer = Recognizer(currentModel, PcmAudioFormat.SAMPLE_RATE.toFloat())
        _state.value = SpeechEngineState.LivePartial("")
        Result.success(Unit)
    }

    override fun acceptPcmChunk(pcmChunk: ByteArray) {
        val recognizer = liveRecognizer ?: return
        if (pcmChunk.isEmpty()) return

        val accepted = recognizer.acceptWaveForm(pcmChunk, pcmChunk.size)
        val json = if (accepted) recognizer.result else recognizer.partialResult
        val text = parseVoskText(json, partial = !accepted)
        if (text.isNotBlank()) {
            if (accepted) partialTexts.add(text)
            _state.value = SpeechEngineState.LivePartial(
                (partialTexts + listOfNotNull(text.takeIf { !accepted })).joinToString(" ").trim()
            )
        }
    }

    override suspend fun finishLiveRecognition(): Result<String> = withContext(Dispatchers.IO) {
        val recognizer = liveRecognizer
            ?: return@withContext Result.failure(IllegalStateException("ضبط زنده فعال نیست"))

        val finalText = parseVoskText(recognizer.finalResult, partial = false)
        if (finalText.isNotBlank()) partialTexts.add(finalText)

        val combined = partialTexts.joinToString(" ").trim()
        recognizer.close()
        liveRecognizer = null
        partialTexts.clear()
        _state.value = SpeechEngineState.Ready(VoskModelManager.DEFAULT_MODEL_ID)
        Result.success(combined)
    }

    override fun cancelLiveRecognition() {
        liveRecognizer?.close()
        liveRecognizer = null
        partialTexts.clear()
    }

    override fun release() {
        cancelLiveRecognition()
        model?.close()
        model = null
        _state.value = SpeechEngineState.NotInitialized
    }

    private suspend fun loadModelOrFail(): Result<Model> {
        val existing = model
        if (existing != null) return Result.success(existing)

        val init = initialize()
        if (init.isFailure) {
            return Result.failure(init.exceptionOrNull() ?: IllegalStateException("مدل آماده نیست"))
        }
        return Result.success(model!!)
    }

    private fun recognizePcm(currentModel: Model, pcmData: ByteArray): String {
        val recognizer = Recognizer(currentModel, PcmAudioFormat.SAMPLE_RATE.toFloat())
        val chunkSize = 4000
        var offset = 0
        val parts = mutableListOf<String>()

        while (offset < pcmData.size) {
            val end = minOf(offset + chunkSize, pcmData.size)
            val chunk = pcmData.copyOfRange(offset, end)
            if (recognizer.acceptWaveForm(chunk, chunk.size)) {
                parseVoskText(recognizer.result, partial = false).takeIf { it.isNotBlank() }?.let(parts::add)
            }
            offset = end
            _state.value = SpeechEngineState.Transcribing(offset.toFloat() / pcmData.size)
        }

        parseVoskText(recognizer.finalResult, partial = false).takeIf { it.isNotBlank() }?.let(parts::add)
        recognizer.close()
        return parts.joinToString(" ").trim()
    }

    private fun readAudioAsPcm16(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists()) return byteArrayOf()

        return when {
            filePath.endsWith(".wav", ignoreCase = true) -> WavFileUtils.readPcm16Mono(file)
            else -> AudioDecoder.decodeToPcm16(context, filePath, PcmAudioFormat.SAMPLE_RATE)
        }
    }

    private fun parseVoskText(json: String, partial: Boolean): String {
        return try {
            val obj = JSONObject(json)
            when {
                partial -> obj.optString("partial", "").trim()
                else -> obj.optString("text", "").trim()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
