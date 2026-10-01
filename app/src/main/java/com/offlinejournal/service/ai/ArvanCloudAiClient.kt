package com.offlinejournal.service.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.offlinejournal.service.ai.ArvanCloudJsonParsing.parseBackendCleanupResponse
import com.offlinejournal.service.ai.ArvanCloudJsonParsing.parseChatCompletionResponse
import com.offlinejournal.service.ai.ArvanCloudJsonParsing.parseErrorMessage
import com.offlinejournal.service.ai.ArvanCloudJsonParsing.parseModelsResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

data class ArvanModelInfo(
    val id: String,
    val ownedBy: String?
)

class ArvanCloudApiException(
    val httpStatus: Int,
    message: String,
    val responseBody: String? = null
) : IOException(message)

/**
 * HTTP client for ArvanCloud AI Gateway (OpenAI-compatible when using direct mode).
 * Supports a future backend proxy URL so production builds never embed API keys.
 */
class ArvanCloudAiClient(
    private val settingsRepository: AiSettingsRepository
) {

    suspend fun listModels(): Result<List<ArvanModelInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val config = settingsRepository.runtimeConfig()
            require(config.isDirectGatewayConfigured) {
                "کلید API یا Base URL برای Gateway تنظیم نشده است"
            }
            val url = URL("${config.baseUrl}/models")
            val response = executeGet(url, config)
            parseModelsResponse(response)
        }
    }

    suspend fun chatCompletion(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val config = settingsRepository.runtimeConfig()
            when {
                config.isBackendProxyConfigured -> {
                    chatViaBackendProxy(config, systemPrompt, userMessage)
                }
                config.isDirectGatewayConfigured -> {
                    chatViaOpenAiCompatibleGateway(config, systemPrompt, userMessage)
                }
                else -> error("تنظیمات AI (کلید API، مدل، یا backend proxy) کامل نیست")
            }
        }
    }

    private fun chatViaOpenAiCompatibleGateway(
        config: AiRuntimeConfig,
        systemPrompt: String,
        userMessage: String
    ): String {
        val url = URL("${config.baseUrl}/chat/completions")
        val body = JSONObject().apply {
            put("model", config.modelId)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
            )
        }
        val response = executePostJson(url, config, body.toString())
        return parseChatCompletionResponse(response)
    }

    /**
     * Future-friendly: POST { "rawText": "...", "systemPrompt": "..." } → { "text": "..." }
     */
    private fun chatViaBackendProxy(
        config: AiRuntimeConfig,
        systemPrompt: String,
        userMessage: String
    ): String {
        val url = URL(config.backendCleanupUrl!!)
        val body = JSONObject().apply {
            put("rawText", userMessage)
            put("systemPrompt", systemPrompt)
            if (config.modelId.isNotBlank()) put("model", config.modelId)
        }
        val response = executePostJson(
            url,
            config.copy(apiKey = null),
            body.toString(),
            authBearer = null
        )
        return parseBackendCleanupResponse(response)
    }

    private fun executeGet(url: URL, config: AiRuntimeConfig): String {
        return executeWithRetry(config) {
            val conn = openConnection(url, config, "GET", authBearer = config.apiKey)
            try {
                readResponseBody(conn)
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun executePostJson(
        url: URL,
        config: AiRuntimeConfig,
        jsonBody: String,
        authBearer: String? = config.apiKey
    ): String {
        return executeWithRetry(config) {
            val conn = openConnection(url, config, "POST", authBearer)
            try {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                }
                readResponseBody(conn)
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun openConnection(
        url: URL,
        config: AiRuntimeConfig,
        method: String,
        authBearer: String?
    ): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = config.connectTimeoutMs
        conn.readTimeout = config.readTimeoutMs
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (!authBearer.isNullOrBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $authBearer")
        }
        return conn
    }

    private fun readResponseBody(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
        if (code !in 200..299) {
            throw ArvanCloudApiException(
                httpStatus = code,
                message = parseErrorMessage(body) ?: "HTTP $code",
                responseBody = body
            )
        }
        return body
    }

    private fun executeWithRetry(config: AiRuntimeConfig, block: () -> String): String {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt <= config.maxRetries) {
            try {
                return block()
            } catch (e: ArvanCloudApiException) {
                lastError = e
                if (!shouldRetry(e.httpStatus) || attempt == config.maxRetries) throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt == config.maxRetries) throw e
            }
            attempt++
            delaySync(min(1000L * (1 shl (attempt - 1)), 4000L))
        }
        throw lastError ?: IOException("درخواست ناموفق بود")
    }

    private fun shouldRetry(httpStatus: Int): Boolean {
        return httpStatus == 429 || httpStatus == 502 || httpStatus == 503 || httpStatus == 504
    }

    private fun delaySync(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

}
