package com.offlinejournal.data.local

import android.content.Context
import androidx.room.Room
import com.offlinejournal.data.repository.CategoryRepository
import com.offlinejournal.data.repository.NoteRepository
import com.offlinejournal.data.repository.ReminderRepository
import com.offlinejournal.service.audio.AudioRecorderManager
import com.offlinejournal.service.market.MarketPriceRepository
import com.offlinejournal.service.backup.BackupManager
import com.offlinejournal.service.reminder.NotificationHelper
import com.offlinejournal.service.reminder.ReminderScheduler
import com.offlinejournal.service.ai.AiSettingsRepository
import com.offlinejournal.service.ai.TranscriptionPipelineCoordinator
import com.offlinejournal.service.ai.AiTextCleanupEngine
import com.offlinejournal.service.ai.ArvanCloudAiClient
import com.offlinejournal.service.ai.ArvanCloudTextCleanupEngine
import com.offlinejournal.service.speech.RemoteSpeechToTextEngine
import com.offlinejournal.service.speech.SpeechToTextEngine
import com.offlinejournal.service.speech.VoskModelManager
import com.offlinejournal.service.speech.VoskSpeechToTextEngine

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext
    private val appContext = applicationContext

    private val database: OfflineJournalDatabase = Room.databaseBuilder(
        appContext,
        OfflineJournalDatabase::class.java,
        "offline_journal.db"
    ).build()

    val categoryRepository = CategoryRepository(database.categoryDao())
    val noteRepository = NoteRepository(database.noteDao())

    private val reminderScheduler = ReminderScheduler(appContext)
    val audioRecorderManager = AudioRecorderManager(appContext)
    val reminderRepository = ReminderRepository(database.reminderDao(), reminderScheduler)
    val backupManager = BackupManager(
        appContext,
        database,
        audioRecorderManager,
        reminderScheduler
    )

    val marketPriceRepository = MarketPriceRepository()
    val voskModelManager = VoskModelManager(appContext)
    /** Offline Persian STT (raw transcription). */
    val speechToTextEngine: SpeechToTextEngine = VoskSpeechToTextEngine(appContext, voskModelManager)
    /** Reserved for future cloud STT providers. */
    val remoteSpeechToTextEngine: SpeechToTextEngine = RemoteSpeechToTextEngine()

    val aiSettingsRepository = AiSettingsRepository(appContext)
    val arvanCloudAiClient = ArvanCloudAiClient(aiSettingsRepository)
    val aiTextCleanupEngine: AiTextCleanupEngine =
        ArvanCloudTextCleanupEngine(arvanCloudAiClient, aiSettingsRepository)
    val transcriptionPipelineCoordinator = TranscriptionPipelineCoordinator(this)

    init {
        NotificationHelper.createNotificationChannel(appContext)
    }
}
