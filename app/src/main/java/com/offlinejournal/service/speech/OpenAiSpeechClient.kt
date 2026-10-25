package com.offlinejournal.service.speech

import com.offlinejournal.service.ai.CloudSpeechSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Uploads the ORIGINAL recording, never the Vosk text. Provider key stays on server. */
class OpenAiSpeechClient(private val settings: CloudSpeechSettings) {
    suspend fun transcribe(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = settings.read() ?: error("ابتدا اتصال OpenAI را تنظیم کنید")
            val file = File(path)
            require(file.isFile && file.length() in 45..(24L * 1024 * 1024)) {
                "فایل صوتی خالی است یا از ۲۴ مگابایت بزرگ‌تر است"
            }
            val conn = URL(config.endpoint).openConnection() as HttpsURLConnection
            try {
                conn.instanceFollowRedirects = false
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 150000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer " + config.token)
                conn.setRequestProperty("Content-Type", "audio/wav")
                conn.setFixedLengthStreamingMode(file.length())
                conn.outputStream.use { out -> file.inputStream().use { it.copyTo(out) } }
                val code = conn.responseCode
                require(code == 200) {
                    when (code) {
                        401 -> "توکن اتصال سرور نامعتبر است"
                        413 -> "فایل صوتی بیش از حد بزرگ است"
                        429 -> "سقف مصرف تبدیل صدا پر شده است"
                        else -> "تبدیل آنلاین انجام نشد (HTTP $code)"
                    }
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val transcript = JSONObject(text).getString("text").trim()
                require(transcript.isNotEmpty()) { "گفتار قابل تشخیصی دریافت نشد" }
                Result.success(transcript)
            } finally { conn.disconnect() }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }
}
