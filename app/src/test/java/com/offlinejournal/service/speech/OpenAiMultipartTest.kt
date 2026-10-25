package com.offlinejournal.service.speech

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class OpenAiMultipartTest {
    @Test fun `streams original audio and reports exact UTF8 byte length`() {
        val file = File.createTempFile("private-note-", ".wav")
        try {
            val audio = byteArrayOf(0, 1, 13, 10, -1, 65)
            file.writeBytes(audio)
            val body = OpenAiMultipart(file, "gpt-transcribe", "test-boundary")
            val out = ByteArrayOutputStream()
            body.writeTo(out)
            val bytes = out.toByteArray()
            assertEquals(bytes.size.toLong(), body.contentLength)
            val text = bytes.toString(Charsets.UTF_8)
            assertTrue(text.contains("name=\"model\"\r\n\r\ngpt-transcribe\r\n"))
            assertTrue(text.contains("filename=\"recording.wav\""))
            assertFalse(text.contains(file.name))
            assertFalse(text.contains("Authorization"))
            val suffix = "\r\n--test-boundary--\r\n".toByteArray()
            assertArrayEquals(audio, bytes.copyOfRange(bytes.size - suffix.size - audio.size, bytes.size - suffix.size))
            assertTrue(text.endsWith("--test-boundary--\r\n"))
        } finally { file.delete() }
    }

    @Test fun `accepts old m4a recordings`() {
        val file = File.createTempFile("old-note-", ".m4a")
        try {
            val out = ByteArrayOutputStream()
            OpenAiMultipart(file, "gpt-transcribe", "test").writeTo(out)
            assertTrue(out.toString("UTF-8").contains("Content-Type: audio/mp4"))
        } finally { file.delete() }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects multipart field injection`() {
        val file = File.createTempFile("note-", ".wav")
        try { OpenAiMultipart(file, "model\r\nattack", "test") } finally { file.delete() }
    }

    @Test fun `requires actual nonempty transcript string`() {
        assertEquals("سلام", parseSpeechResponse("{\"text\":\" سلام \"}"))
        for (json in listOf("{}", "{\"text\":null}", "{\"text\":123}", "{\"text\":\" \"}")) {
            assertTrue(runCatching { parseSpeechResponse(json) }.isFailure)
        }
    }

    @Test fun `direct auth errors differ from proxy errors`() {
        assertTrue(speechHttpError(401, true).contains("OpenAI"))
        assertTrue(speechHttpError(401, false).contains("توکن"))
        assertTrue(speechHttpError(429, true).contains("اعتبار"))
    }
}
