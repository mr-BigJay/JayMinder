package com.offlinejournal.service.ai

/**
 * Runtime configuration for ArvanCloud / AI cleanup.
 * Secrets (API key) are loaded from local DataStore or a future backend — never from source control.
 */
data class AiRuntimeConfig(
    val baseUrl: String,
    val modelId: String,
    val apiKey: String?,
    val backendCleanupUrl: String?,
    val connectTimeoutMs: Int = 15_000,
    val readTimeoutMs: Int = 60_000,
    val maxRetries: Int = 2
) {
    val isDirectGatewayConfigured: Boolean
        get() = !apiKey.isNullOrBlank() && baseUrl.isNotBlank() && modelId.isNotBlank()

    val isBackendProxyConfigured: Boolean
        get() = !backendCleanupUrl.isNullOrBlank()
}
