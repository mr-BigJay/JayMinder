package com.offlinejournal.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.offlinejournal.OfflineJournalApp

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآوری"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, reminderId.toInt())

        NotificationHelper.showReminderNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            description = description,
            reminderId = reminderId
        )
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
