package com.offlinejournal.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.offlinejournal.OfflineJournalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as OfflineJournalApp
            scope.launch {
                app.container.reminderRepository.rescheduleAllActive()
            }
        }
    }
}
