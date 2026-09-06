package com.offlinejournal.data.mapper

import com.offlinejournal.data.local.entity.CategoryEntity
import com.offlinejournal.data.local.entity.NoteEntity
import com.offlinejournal.data.local.entity.ReminderEntity
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import com.offlinejournal.domain.model.Reminder

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colorArgb = colorArgb,
    createdAtMillis = createdAtMillis
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    createdAtMillis = createdAtMillis
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    textContent = textContent,
    audioFilePath = audioFilePath,
    transcription = transcription,
    categoryId = categoryId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    textContent = textContent,
    audioFilePath = audioFilePath,
    transcription = transcription,
    categoryId = categoryId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    noteId = noteId,
    scheduledAtMillis = scheduledAtMillis,
    isCompleted = isCompleted,
    notificationId = notificationId,
    createdAtMillis = createdAtMillis
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    description = description,
    noteId = noteId,
    scheduledAtMillis = scheduledAtMillis,
    isCompleted = isCompleted,
    notificationId = notificationId,
    createdAtMillis = createdAtMillis
)
