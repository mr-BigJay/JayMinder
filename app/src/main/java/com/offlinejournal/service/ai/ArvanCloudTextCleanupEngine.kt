package com.offlinejournal.service.ai

class ArvanCloudTextCleanupEngine(
    private val client: ArvanCloudAiClient,
    private val settingsRepository: AiSettingsRepository
) : AiTextCleanupEngine {

    override suspend fun cleanText(rawText: String): Result<String> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("متن خام خالی است"))
        }

        val config = settingsRepository.runtimeConfig()
        if (!config.isBackendProxyConfigured && !config.isDirectGatewayConfigured) {
            return Result.failure(IllegalStateException("تنظیمات ArvanCloud (API Key و مدل) کامل نیست"))
        }
        if (config.modelId.isBlank() && !config.isBackendProxyConfigured) {
            return Result.failure(IllegalStateException("شناسه مدل (ARVAN_MODEL) تنظیم نشده است"))
        }

        return client.chatCompletion(
            systemPrompt = AiTextCleanupPrompt.SYSTEM_PROMPT,
            userMessage = trimmed
        ).map { cleaned ->
            cleaned.trim().ifBlank { trimmed }
        }
    }
}
