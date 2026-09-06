package com.offlinejournal.data.local

import android.content.Context
import androidx.room.Room
import com.offlinejournal.data.repository.CategoryRepository
import com.offlinejournal.data.repository.NoteRepository
import com.offlinejournal.data.repository.ReminderRepository
import com.offlinejournal.service.audio.AudioRecorderManager
import com.offlinejournal.service.reminder.NotificationHelper
import com.offlinejournal.service.reminder.ReminderScheduler
import com.offlinejournal.service.speech.SpeechToTextEngine
import com.offlinejournal.service.speech.VoskSpeechToTextEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: OfflineJournalDatabase = Room.databaseBuilder(
        appContext,
        OfflineJournalDatabase::class.java,
        "offline_journal.db"
    ).build()

    val categoryRepository = CategoryRepository(database.categoryDao())
    val noteRepository = NoteRepository(database.noteDao())

    private val reminderScheduler = ReminderScheduler(appContext)
    val reminderRepository = ReminderRepository(database.reminderDao(), reminderScheduler)

    val audioRecorderManager = AudioRecorderManager(appContext)
    val speechToTextEngine: SpeechToTextEngine = VoskSpeechToTextEngine(appContext)

    init {
        NotificationHelper.createNotificationChannel(appContext)
    }
}
