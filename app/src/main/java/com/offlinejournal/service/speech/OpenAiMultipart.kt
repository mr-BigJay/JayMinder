package com.offlinejournal.service.speech

import java.io.File
import java.io.OutputStream
import org.json.JSONObject

/** Streaming multipart encoder: no API key, local path or user filename in the body. */
class OpenAiMultipart(private val file: File, model: String, val boundary: String) {
    private val format = when (file.extension.lowercase()) {
        "wav" -> "wav" to "audio/wav"
        "m4a" -> "m4a" to "audio/mp4"
        "mp3" -> "mp3" to "audio/mpeg"
        "mp4" -> "mp4" to "audio/mp4"
        "webm" -> "webm" to "audio/webm"
        "mpeg", "mpga" -> "mpeg" to "audio/mpeg"
        else -> throw IllegalArgumentException("فرمت فایل صوتی پشتیبانی نمی‌شود")
    }
    init {
        require(model.matches(Regex("[A-Za-z0-9._-]{1,100}")))
        require(boundary.matches(Regex("[A-Za-z0-9-]{1,100}")))
    }
    private val prefix = (
        "--$boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n$model\r\n" +
        "--$boundary\r\nContent-Disposition: form-data; name=\"prompt\"\r\n\r\n" +
        "گفتار فارسی؛ نام‌ها، اعداد و تاریخ‌ها را دقیق ثبت کن. متن را خلاصه نکن.\r\n" +
        "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"recording.${format.first}\"\r\n" +
        "Content-Type: ${format.second}\r\n\r\n"
    ).toByteArray(Charsets.UTF_8)
    private val suffix = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
    val contentLength: Long get() = prefix.size + file.length() + suffix.size
    fun writeTo(out: OutputStream) {
        out.write(prefix)
        file.inputStream().use { it.copyTo(out) }
        out.write(suffix)
    }
}

fun parseSpeechResponse(body: String): String {
    val value = JSONObject(body).opt("text")
    require(value is String && value.isNotBlank()) { "گفتار قابل تشخیصی دریافت نشد" }
    return value.trim()
}

fun speechHttpError(code: Int, direct: Boolean): String = when (code) {
    401 -> if (direct) "کلید OpenAI نامعتبر یا لغو شده است" else "توکن اتصال سرور نامعتبر است"
    403 -> "دسترسی به سرویس یا مدل مجاز نیست"
    404 -> "مدل یا آدرس سرویس در دسترس نیست؛ تنظیمات را بررسی کنید"
    413 -> "فایل صوتی بیش از حد بزرگ است"
    429 -> if (direct) "اعتبار API یا سهمیه درخواست‌ها را در حساب OpenAI بررسی کنید" else "سقف مصرف تبدیل صدا پر شده است"
    else -> "تبدیل آنلاین انجام نشد (HTTP $code)"
}
