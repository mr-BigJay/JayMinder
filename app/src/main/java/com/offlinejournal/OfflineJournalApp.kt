package com.offlinejournal

import android.app.Application
import com.offlinejournal.data.local.AppContainer

class OfflineJournalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
