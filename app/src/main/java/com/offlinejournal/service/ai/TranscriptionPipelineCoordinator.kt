package com.offlinejournal.service.ai

import android.content.Context
import com.offlinejournal.data.local.AppContainer
import kotlinx.coroutines.flow.first

/**
 * Vosk raw text → optional ArvanCloud cleanup, with offline fallback.
 */
class TranscriptionPipelineCoordinator(
    private val container: AppContainer
) {
    private val appContext: Context get() = container.applicationContext

    suspend fun transcribeFile(path: String, onStatus: (TranscriptionPipelineStatus) -> Unit): Pair<String, String?> {
        if (container.aiSettingsRepository.transcriptionMode.first() == TranscriptionPipelineMode.OPENAI) {
            return finalizeTranscription("", path, onStatus)
        }
        val ready = container.speechToTextEngine.ensureModelInstalled()
        if (ready.isFailure) return "" to "مدل آفلاین آماده نیست"
        val result = container.speechToTextEngine.transcribeFile(path)
        if (result.isFailure) return "" to "تبدیل آفلاین ناموفق بود"
        return finalizeTranscription(result.getOrDefault(""), path, onStatus)
    }

    suspend fun finalizeTranscription(
        rawVoskText: String,
        audioFilePath: String? = null,
        onStatus: (TranscriptionPipelineStatus) -> Unit
    ): Pair<String, String?> {
        val mode = container.aiSettingsRepository.transcriptionMode.first()
        if (mode == TranscriptionPipelineMode.OPENAI) {
            onStatus(TranscriptionPipelineStatus.ConvertingSpeech)
            val result = if (audioFilePath != null) container.openAiSpeechClient.transcribe(audioFilePath)
                else Result.failure(IllegalStateException("فایل صوتی موجود نیست"))
            result.getOrNull()?.let {
                onStatus(TranscriptionPipelineStatus.Completed)
                return it to null
            }
            var fallback = rawVoskText.trim()
            if (fallback.isBlank() && audioFilePath != null) {
                if (container.speechToTextEngine.ensureModelInstalled().isSuccess) {
                    fallback = container.speechToTextEngine.transcribeFile(audioFilePath).getOrDefault("")
                }
            }
            val message = if (fallback.isNotBlank()) "تبدیل آنلاین ناموفق بود؛ متن آفلاین حفظ شد."
                else "تبدیل آنلاین ناموفق بود؛ فایل صوتی حفظ می‌شود."
            onStatus(TranscriptionPipelineStatus.SmartCleanupUnavailable(message))
            return fallback to (message + " " + (result.exceptionOrNull()?.message ?: ""))
        }
        val trimmedRaw = rawVoskText.trim()
        if (trimmedRaw.isEmpty()) {
            onStatus(TranscriptionPipelineStatus.Idle)
            return rawVoskText to null
        }

        if (mode != TranscriptionPipelineMode.AI) {
            onStatus(TranscriptionPipelineStatus.Completed)
            return trimmedRaw to null
        }

        if (!NetworkConnectivity.isOnline(appContext)) {
            onStatus(
                TranscriptionPipelineStatus.SmartCleanupUnavailable(
                    "بهبود هوشمند در دسترس نیست؛ متن آفلاین ذخیره شد."
                )
            )
            return trimmedRaw to null
        }

        val config = container.aiSettingsRepository.runtimeConfig()
        if (!config.isBackendProxyConfigured && !config.isDirectGatewayConfigured) {
            onStatus(
                TranscriptionPipelineStatus.SmartCleanupUnavailable(
                    "بهبود هوشمند در دسترس نیست؛ متن آفلاین ذخیره شد."
                )
            )
            return trimmedRaw to "کلید API ArvanCloud روی دستگاه تنظیم نشده است."
        }

        onStatus(TranscriptionPipelineStatus.ImprovingText)

        return container.aiTextCleanupEngine.cleanText(trimmedRaw).fold(
            onSuccess = { cleaned ->
                onStatus(TranscriptionPipelineStatus.Completed)
                cleaned.ifBlank { trimmedRaw } to null
            },
            onFailure = { error ->
                onStatus(
                    TranscriptionPipelineStatus.SmartCleanupUnavailable(
                        "بهبود هوشمند در دسترس نیست؛ متن آفلاین ذخیره شد."
                    )
                )
                trimmedRaw to (error.message ?: "بهبود متن ناموفق بود")
            }
        )
    }
}
