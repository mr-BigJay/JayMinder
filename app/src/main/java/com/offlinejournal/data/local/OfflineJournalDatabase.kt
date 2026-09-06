package com.offlinejournal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.offlinejournal.data.local.dao.CategoryDao
import com.offlinejournal.data.local.dao.NoteDao
import com.offlinejournal.data.local.dao.ReminderDao
import com.offlinejournal.data.local.entity.CategoryEntity
import com.offlinejournal.data.local.entity.NoteEntity
import com.offlinejournal.data.local.entity.ReminderEntity

@Database(
    entities = [
        CategoryEntity::class,
        NoteEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OfflineJournalDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
}
