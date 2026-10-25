package com.offlinejournal.service.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class CloudSpeechConfig(
    val endpoint: String,
    val token: String,
    val direct: Boolean = false,
    val model: String = "gpt-transcribe"
)

class CloudSpeechSettings(context: Context) {
    private val file = File(context.noBackupFilesDir, "cloud-speech.enc")
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("jayminder-cloud", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("jayminder-cloud",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    fun save(endpoint: String, token: String) {
        val uri = URI(endpoint.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.query == null && uri.fragment == null) { "آدرس کامل HTTPS سرور را وارد کنید" }
        require(token.trim().length >= 32 && !token.contains('\n') && !token.contains('\r')) {
            "توکن اتصال باید حداقل ۳۲ کاراکتر باشد"
        }
        writeConfig(CloudSpeechConfig(endpoint.trim(), token.trim()))
    }
    fun saveDirect(apiKey: String, model: String) {
        val cleanKey = apiKey.trim()
        require(cleanKey.startsWith("sk-") && cleanKey.length >= 20 && cleanKey.none { it.isWhitespace() }) {
            "کلید OpenAI معتبر را وارد کنید"
        }
        val cleanModel = model.trim()
        require(cleanModel.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "نام مدل معتبر نیست" }
        writeConfig(CloudSpeechConfig("https://api.openai.com/v1/audio/transcriptions", cleanKey, true, cleanModel))
    }
    fun clear() { android.util.AtomicFile(file).delete() }
    private fun writeConfig(config: CloudSpeechConfig) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val json = JSONObject().put("endpoint", config.endpoint).put("token", config.token)
            .put("direct", config.direct).put("model", config.model).toString()
        val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val atomic = android.util.AtomicFile(file)
        val out = atomic.startWrite()
        try {
            out.write((Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(out)
        } catch (e: Exception) {
            atomic.failWrite(out)
            throw e
        }
    }
    fun read(): CloudSpeechConfig? = runCatching {
        val parts = android.util.AtomicFile(file).openRead().bufferedReader().use { it.readText() }.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        }
        val json = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
        CloudSpeechConfig(json.getString("endpoint"), json.getString("token"),
            json.optBoolean("direct", false), json.optString("model", "gpt-transcribe"))
    }.getOrNull()
}
