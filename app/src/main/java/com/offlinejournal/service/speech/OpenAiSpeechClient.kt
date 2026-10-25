package com.offlinejournal.service.speech

import com.offlinejournal.service.ai.CloudSpeechSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.UUID
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Sends original audio either directly to OpenAI or to the optional personal gateway. */
class OpenAiSpeechClient(private val settings: CloudSpeechSettings) {
    suspend fun transcribe(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = settings.read() ?: error("ابتدا اتصال OpenAI را تنظیم کنید")
            val file = File(path)
            require(file.isFile && file.length() in 45..(24L * 1024 * 1024)) {
                "فایل صوتی خالی است یا از ۲۴ مگابایت بزرگ‌تر است"
            }
            // A provider API key can only go to the fixed official endpoint, never to the proxy URL.
            val endpoint = if (config.direct) "https://api.openai.com/v1/audio/transcriptions" else config.endpoint
            val conn = URL(endpoint).openConnection() as HttpsURLConnection
            try {
                conn.instanceFollowRedirects = false
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 150000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer " + config.token)
                if (config.direct) {
                    val multipart = OpenAiMultipart(file, config.model, "JayMinder-" + UUID.randomUUID())
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + multipart.boundary)
                    conn.setFixedLengthStreamingMode(multipart.contentLength)
                    conn.outputStream.use { multipart.writeTo(it) }
                } else {
                    conn.setRequestProperty("Content-Type", "audio/wav")
                    conn.setFixedLengthStreamingMode(file.length())
                    conn.outputStream.use { out -> file.inputStream().use { it.copyTo(out) } }
                }
                val code = conn.responseCode
                require(code == 200) { speechHttpError(code, config.direct) }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val transcript = parseSpeechResponse(text)
                Result.success(transcript)
            } finally { conn.disconnect() }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }
}
