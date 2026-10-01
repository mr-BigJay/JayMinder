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

    suspend fun finalizeTranscription(
        rawVoskText: String,
        onStatus: (TranscriptionPipelineStatus) -> Unit
    ): Pair<String, String?> {
        val trimmedRaw = rawVoskText.trim()
        if (trimmedRaw.isEmpty()) {
            onStatus(TranscriptionPipelineStatus.Idle)
            return rawVoskText to null
        }

        val mode = container.aiSettingsRepository.transcriptionMode.first()
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
