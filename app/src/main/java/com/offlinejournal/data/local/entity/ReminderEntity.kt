package com.offlinejournal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("noteId"),
        Index("scheduledAtMillis"),
        Index("isCompleted")
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val noteId: Long? = null,
    val scheduledAtMillis: Long,
    val isCompleted: Boolean = false,
    val notificationId: Int,
    val createdAtMillis: Long
)
