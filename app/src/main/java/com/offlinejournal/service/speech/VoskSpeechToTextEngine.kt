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
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Offline Persian speech recognition using Vosk.
 * Model must be placed in assets under model/vosk-model-small-fa-0.5/
 */
class VoskSpeechToTextEngine(
    private val context: Context,
    private val modelAssetPath: String = "model/vosk-model-small-fa-0.5"
) : SpeechToTextEngine {

    private val _state = MutableStateFlow<SpeechEngineState>(SpeechEngineState.NotInitialized)
    override val state: StateFlow<SpeechEngineState> = _state.asStateFlow()

    private var model: Model? = null

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (model != null) {
            _state.value = SpeechEngineState.Ready(modelAssetPath)
            return@withContext Result.success(Unit)
        }

        _state.value = SpeechEngineState.Initializing
        try {
            val modelDir = unpackModelIfNeeded()
            model = Model(modelDir.absolutePath)
            _state.value = SpeechEngineState.Ready(modelAssetPath)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = SpeechEngineState.Error(
                "خطا در بارگذاری مدل تشخیص گفتار. لطفاً مدل Vosk فارسی را نصب کنید."
            )
            Result.failure(e)
        }
    }

    override suspend fun transcribeFile(audioFilePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            val currentModel = model ?: run {
                val initResult = initialize()
                if (initResult.isFailure) {
                    return@withContext Result.failure(
                        initResult.exceptionOrNull() ?: IllegalStateException("مدل آماده نیست")
                    )
                }
                model!!
            }

            _state.value = SpeechEngineState.Transcribing(0f)
            try {
                val recognizer = Recognizer(currentModel, 16000.0f)
                val pcmData = decodeToPcm16(audioFilePath)
                if (pcmData.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("فایل صوتی خالی است"))
                }

                val chunkSize = 4000
                var offset = 0
                while (offset < pcmData.size) {
                    val end = minOf(offset + chunkSize, pcmData.size)
                    val chunk = pcmData.copyOfRange(offset, end)
                    recognizer.acceptWaveForm(chunk, chunk.size)
                    offset = end
                    _state.value = SpeechEngineState.Transcribing(offset.toFloat() / pcmData.size)
                }

                val finalResult = recognizer.finalResult
                val text = parseVoskText(finalResult)
                recognizer.close()
                _state.value = SpeechEngineState.Ready(modelAssetPath)
                Result.success(text)
            } catch (e: Exception) {
                _state.value = SpeechEngineState.Error("خطا در تبدیل صدا به متن: ${e.message}")
                Result.failure(e)
            }
        }

    override fun release() {
        model?.close()
        model = null
        _state.value = SpeechEngineState.NotInitialized
    }

    private fun unpackModelIfNeeded(): File {
        val targetDir = File(context.filesDir, "vosk-model")
        val marker = File(targetDir, ".installed")
        if (marker.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            return targetDir
        }

        targetDir.deleteRecursively()
        targetDir.mkdirs()

        val assetManager = context.assets
        val assetPath = modelAssetPath
        try {
            val assets = assetManager.list(assetPath)
            if (assets == null || assets.isEmpty()) {
                throw IllegalStateException("مدل در assets پیدا نشد: $assetPath")
            }
            copyAssetFolder(assetManager, assetPath, targetDir)
            marker.writeText("ok")
            return targetDir
        } catch (e: Exception) {
            // Fallback to StorageService if available
            throw IllegalStateException(
                "مدل Vosk فارسی در assets موجود نیست. لطفاً vosk-model-small-fa-0.5 را در assets/model قرار دهید.",
                e
            )
        }
    }

    private fun copyAssetFolder(
        assetManager: android.content.res.AssetManager,
        assetPath: String,
        targetDir: File
    ) {
        val files = assetManager.list(assetPath) ?: return
        if (files.isEmpty()) {
            assetManager.open(assetPath).use { input ->
                targetDir.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        targetDir.mkdirs()
        for (file in files) {
            val subPath = "$assetPath/$file"
            val subTarget = File(targetDir, file)
            val subFiles = assetManager.list(subPath)
            if (subFiles != null && subFiles.isNotEmpty()) {
                copyAssetFolder(assetManager, subPath, subTarget)
            } else {
                assetManager.open(subPath).use { input ->
                    subTarget.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun decodeToPcm16(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists()) return byteArrayOf()

        // For WAV files, skip header and read PCM data
        if (filePath.endsWith(".wav", ignoreCase = true)) {
            return readWavPcm(file)
        }

        // For m4a/aac, use Android MediaExtractor + MediaCodec to decode
        return AudioDecoder.decodeToPcm16(context, filePath, sampleRate = 16000)
    }

    private fun readWavPcm(file: File): ByteArray {
        FileInputStream(file).use { fis ->
            val header = ByteArray(44)
            if (fis.read(header) < 44) return byteArrayOf()
            val dataSize = ByteBuffer.wrap(header, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val buffer = ByteArray(dataSize)
            var read = 0
            while (read < dataSize) {
                val count = fis.read(buffer, read, dataSize - read)
                if (count <= 0) break
                read += count
            }
            return buffer.copyOf(read)
        }
    }

    private fun parseVoskText(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", "").trim()
        } catch (_: Exception) {
            ""
        }
    }
}
