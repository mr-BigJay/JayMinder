package com.offlinejournal.data.repository

import com.offlinejournal.data.local.dao.ReminderDao
import com.offlinejournal.data.mapper.toDomain
import com.offlinejournal.data.mapper.toEntity
import com.offlinejournal.domain.model.Reminder
import com.offlinejournal.service.reminder.ReminderScheduler
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicInteger

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) {
    private val notificationIdCounter = AtomicInteger(1000)

    fun observeAllReminders(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeUpcomingReminders(): Flow<List<Reminder>> =
        reminderDao.observeUpcoming().map { list -> list.map { it.toDomain() } }

    fun observePastReminders(): Flow<List<Reminder>> =
        reminderDao.observePast().map { list -> list.map { it.toDomain() } }

    suspend fun getReminder(id: Long): Reminder? =
        reminderDao.getById(id)?.toDomain()

    suspend fun createReminder(
        title: String,
        description: String = "",
        noteId: Long? = null,
        scheduledAtMillis: Long
    ): Long {
        val reminder = Reminder(
            title = title.trim(),
            description = description.trim(),
            noteId = noteId,
            scheduledAtMillis = scheduledAtMillis,
            notificationId = notificationIdCounter.incrementAndGet(),
            createdAtMillis = TehranTime.nowMillis()
        )
        val id = reminderDao.insert(reminder.toEntity())
        val saved = reminder.copy(id = id)
        reminderScheduler.schedule(saved)
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.update(reminder.toEntity())
        if (!reminder.isCompleted) {
            reminderScheduler.schedule(reminder)
        } else {
            reminderScheduler.cancel(reminder)
        }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderScheduler.cancel(reminder)
        reminderDao.delete(reminder.toEntity())
    }

    suspend fun markCompleted(reminder: Reminder) {
        updateReminder(reminder.copy(isCompleted = true))
    }

    suspend fun rescheduleAllActive() {
        val now = TehranTime.nowMillis()
        reminderDao.getActiveReminders(now).forEach { entity ->
            val reminder = entity.toDomain()
            if (reminder.scheduledAtMillis > now) {
                reminderScheduler.schedule(reminder)
            }
        }
    }
}
