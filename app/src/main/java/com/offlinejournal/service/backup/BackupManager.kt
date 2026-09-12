package com.offlinejournal.service.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.withTransaction
import com.offlinejournal.data.local.OfflineJournalDatabase
import com.offlinejournal.data.mapper.toDomain
import com.offlinejournal.data.mapper.toEntity
import com.offlinejournal.domain.model.Note
import com.offlinejournal.service.audio.AudioRecorderManager
import com.offlinejournal.service.reminder.ReminderScheduler
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: OfflineJournalDatabase,
    private val audioRecorderManager: AudioRecorderManager,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun exportToDownloads(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = database.categoryDao().getAllOnce().map { it.toDomain() }
            val notes = database.noteDao().getAllOnce().map { it.toDomain() }
            val reminders = database.reminderDao().getAllOnce().map { it.toDomain() }

            val payload = JSONObject().apply {
                put("formatVersion", 1)
                put("exportedAtMillis", TehranTime.nowMillis())
                put("categories", JSONArray().apply {
                    categories.forEach { cat ->
                        put(
                            JSONObject().apply {
                                put("id", cat.id)
                                put("name", cat.name)
                                put("colorArgb", cat.colorArgb)
                                put("createdAtMillis", cat.createdAtMillis)
                            }
                        )
                    }
                })
                put("notes", JSONArray().apply {
                    notes.forEach { note ->
                        put(
                            JSONObject().apply {
                                put("id", note.id)
                                put("title", note.title)
                                put("textContent", note.textContent)
                                put("transcription", note.transcription)
                                put("categoryId", note.categoryId)
                                put("createdAtMillis", note.createdAtMillis)
                                put("updatedAtMillis", note.updatedAtMillis)
                                put("audioZipEntry", note.audioFilePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) "audio/note_${note.id}_${file.name}" else null
                                })
                            }
                        )
                    }
                })
                put("reminders", JSONArray().apply {
                    reminders.forEach { reminder ->
                        put(
                            JSONObject().apply {
                                put("id", reminder.id)
                                put("title", reminder.title)
                                put("description", reminder.description)
                                put("noteId", reminder.noteId)
                                put("scheduledAtMillis", reminder.scheduledAtMillis)
                                put("isCompleted", reminder.isCompleted)
                                put("createdAtMillis", reminder.createdAtMillis)
                            }
                        )
                    }
                })
            }

            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val displayName = "JayMinder-backup-$stamp.zip"

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Files.getContentUri("external")
            }
            val uri = resolver.insert(collection, values)
                ?: error("نمی‌توان فایل پشتیبان در دانلودها ساخت")

            resolver.openOutputStream(uri)?.use { rawOut ->
                ZipOutputStream(rawOut).use { zip ->
                    zip.putNextEntry(ZipEntry("backup.json"))
                    zip.write(payload.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    notes.forEach { note ->
                        val path = note.audioFilePath ?: return@forEach
                        val file = File(path)
                        if (!file.exists()) return@forEach
                        val entryName = "audio/note_${note.id}_${file.name}"
                        zip.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: error("خطا در نوشتن فایل پشتیبان")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            displayName
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tempZip = File(context.cacheDir, "restore-incoming.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempZip.outputStream().use { output -> input.copyTo(output) }
            } ?: error("فایل پشتیبان خوانده نشد")

            val tempDir = File(context.cacheDir, "restore-unpack").apply {
                deleteRecursively()
                mkdirs()
            }
            unzip(tempZip, tempDir)
            val jsonFile = File(tempDir, "backup.json")
            if (!jsonFile.exists()) error("فایل پشتیبان معتبر نیست")

            val root = JSONObject(jsonFile.readText())
            if (root.optInt("formatVersion", 0) != 1) {
                error("نسخه فایل پشتیبان پشتیبانی نمی‌شود")
            }

            val categoryDao = database.categoryDao()
            val noteDao = database.noteDao()
            val reminderDao = database.reminderDao()

            database.withTransaction {
                val existingReminders = reminderDao.getAllOnce()
                existingReminders.forEach { reminderScheduler.cancel(it.toDomain()) }
                val existingNotes = noteDao.getAllOnce()
                val existingCategories = categoryDao.getAllOnce()

                existingReminders.forEach { reminderDao.delete(it) }
                existingNotes.forEach { note ->
                    audioRecorderManager.deleteAudioFile(note.toDomain().audioFilePath)
                    noteDao.delete(note)
                }
                existingCategories.forEach { categoryDao.delete(it) }

                val categoryIdMap = mutableMapOf<Long, Long>()
                val categories = root.getJSONArray("categories")
                for (i in 0 until categories.length()) {
                    val obj = categories.getJSONObject(i)
                    val oldId = obj.getLong("id")
                    val newId = categoryDao.insert(
                        com.offlinejournal.domain.model.Category(
                            name = obj.getString("name"),
                            colorArgb = obj.getInt("colorArgb"),
                            createdAtMillis = obj.getLong("createdAtMillis")
                        ).toEntity()
                    )
                    categoryIdMap[oldId] = newId
                }

                val noteIdMap = mutableMapOf<Long, Long>()
                val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
                val notes = root.getJSONArray("notes")
                for (i in 0 until notes.length()) {
                    val obj = notes.getJSONObject(i)
                    val oldId = obj.getLong("id")
                    val oldCategoryId = obj.optLong("categoryId").takeIf { obj.has("categoryId") && !obj.isNull("categoryId") }
                    val audioZipEntry = obj.optString("audioZipEntry").takeIf { it.isNotBlank() }
                    var audioPath: String? = null
                    if (audioZipEntry != null) {
                        val source = File(tempDir, audioZipEntry)
                        if (source.exists()) {
                            val dest = File(recordingsDir, "restored_${oldId}_${source.name}")
                            source.copyTo(dest, overwrite = true)
                            audioPath = dest.absolutePath
                        }
                    }
                    val note = Note(
                        title = obj.optString("title").takeIf { it.isNotBlank() },
                        textContent = obj.optString("textContent"),
                        transcription = obj.optString("transcription").takeIf { it.isNotBlank() },
                        categoryId = oldCategoryId?.let { categoryIdMap[it] },
                        audioFilePath = audioPath,
                        createdAtMillis = obj.getLong("createdAtMillis"),
                        updatedAtMillis = obj.getLong("updatedAtMillis")
                    )
                    val newId = noteDao.insert(note.toEntity())
                    noteIdMap[oldId] = newId
                }

                val reminders = root.getJSONArray("reminders")
                var notificationSeq = 2000
                for (i in 0 until reminders.length()) {
                    val obj = reminders.getJSONObject(i)
                    val oldNoteId = obj.optLong("noteId").takeIf { obj.has("noteId") && !obj.isNull("noteId") }
                    val reminder = com.offlinejournal.domain.model.Reminder(
                        title = obj.getString("title"),
                        description = obj.optString("description"),
                        noteId = oldNoteId?.let { noteIdMap[it] },
                        scheduledAtMillis = obj.getLong("scheduledAtMillis"),
                        isCompleted = obj.optBoolean("isCompleted"),
                        notificationId = notificationSeq++,
                        createdAtMillis = obj.getLong("createdAtMillis")
                    )
                    val id = reminderDao.insert(reminder.toEntity())
                    val saved = reminder.copy(id = id)
                    if (!saved.isCompleted && saved.scheduledAtMillis > TehranTime.nowMillis()) {
                        reminderScheduler.schedule(saved)
                    }
                }
            }

            tempZip.delete()
            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun unzip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
