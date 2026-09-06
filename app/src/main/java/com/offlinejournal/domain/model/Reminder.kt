package com.offlinejournal.domain.model

data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val noteId: Long? = null,
    val scheduledAtMillis: Long,
    val isCompleted: Boolean = false,
    val notificationId: Int,
    val createdAtMillis: Long
)
