package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Reminder
import com.offlinejournal.util.PersianFormatter
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderEditorUiState(
    val title: String = "",
    val description: String = "",
    val jalaliYear: Int = 0,
    val jalaliMonth: Int = 0,
    val jalaliDay: Int = 0,
    val hour: Int = 9,
    val minute: Int = 0,
    val dateFieldsResetKey: Int = 0,
    val timeFieldsResetKey: Int = 0,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

class RemindersViewModel(private val container: AppContainer) : ViewModel() {
    val upcomingReminders: StateFlow<List<Reminder>> =
        container.reminderRepository.observeUpcomingReminders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastReminders: StateFlow<List<Reminder>> =
        container.reminderRepository.observePastReminders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            container.reminderRepository.deleteReminder(reminder)
        }
    }

    fun markCompleted(reminder: Reminder) {
        viewModelScope.launch {
            container.reminderRepository.markCompleted(reminder)
        }
    }

    fun formatReminderTime(reminder: Reminder): String =
        PersianFormatter.formatDateTime(reminder.scheduledAtMillis)

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemindersViewModel(container) as T
        }
    }
}

class ReminderEditorViewModel(
    private val container: AppContainer,
    private val existingReminderId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(createDefaultState())
    val uiState: StateFlow<ReminderEditorUiState> = _uiState.asStateFlow()

    init {
        if (existingReminderId != null) {
            loadReminder(existingReminderId)
        }
    }

    private fun createDefaultState(): ReminderEditorUiState {
        val now = TehranTime.nowZoned()
        val jalali = com.offlinejournal.util.JalaliCalendar.fromMillis(now.toInstant().toEpochMilli())
        return ReminderEditorUiState(
            jalaliYear = jalali.year,
            jalaliMonth = jalali.month,
            jalaliDay = jalali.day,
            hour = now.hour,
            minute = now.minute
        )
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            container.reminderRepository.getReminder(id)?.let { reminder ->
                val jalali = com.offlinejournal.util.JalaliCalendar.fromMillis(reminder.scheduledAtMillis)
                val zdt = TehranTime.toZonedDateTime(reminder.scheduledAtMillis)
                _uiState.update {
                    it.copy(
                        title = reminder.title,
                        description = reminder.description,
                        jalaliYear = jalali.year,
                        jalaliMonth = jalali.month,
                        jalaliDay = jalali.day,
                        hour = zdt.hour,
                        minute = zdt.minute,
                        dateFieldsResetKey = it.dateFieldsResetKey + 1,
                        timeFieldsResetKey = it.timeFieldsResetKey + 1
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun updateDate(year: Int, month: Int, day: Int) =
        _uiState.update { it.copy(jalaliYear = year, jalaliMonth = month, jalaliDay = day) }
    fun updateTime(hour: Int, minute: Int) =
        _uiState.update { it.copy(hour = hour, minute = minute) }

    fun saveReminder() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "عنوان یادآوری الزامی است") }
            return
        }
        if (state.jalaliYear < 1300 || state.jalaliYear > 1500) {
            _uiState.update { it.copy(errorMessage = "سال شمسی معتبر نیست") }
            return
        }
        if (state.jalaliMonth !in 1..12) {
            _uiState.update { it.copy(errorMessage = "ماه باید بین ۱ تا ۱۲ باشد") }
            return
        }
        val maxDay = com.offlinejournal.util.JalaliCalendar.daysInJalaliMonth(
            state.jalaliYear,
            state.jalaliMonth
        )
        if (state.jalaliDay !in 1..maxDay) {
            _uiState.update { it.copy(errorMessage = "روز برای این ماه معتبر نیست") }
            return
        }
        if (state.hour !in 0..23) {
            _uiState.update { it.copy(errorMessage = "ساعت باید بین ۰ تا ۲۳ باشد") }
            return
        }
        if (state.minute !in 0..59) {
            _uiState.update { it.copy(errorMessage = "دقیقه باید بین ۰ تا ۵۹ باشد") }
            return
        }

        viewModelScope.launch {
            try {
                val jalali = com.offlinejournal.util.JalaliDate(
                    state.jalaliYear, state.jalaliMonth, state.jalaliDay
                )
                val (gy, gm, gd) = com.offlinejournal.util.JalaliCalendar.toGregorian(jalali)
                val zdt = java.time.LocalDateTime.of(gy, gm, gd, state.hour, state.minute)
                    .atZone(TehranTime.zoneId)
                val scheduledMillis = zdt.toInstant().toEpochMilli()

                if (existingReminderId != null) {
                    val existing = container.reminderRepository.getReminder(existingReminderId)
                    if (existing != null) {
                        container.reminderRepository.updateReminder(
                            existing.copy(
                                title = state.title.trim(),
                                description = state.description.trim(),
                                scheduledAtMillis = scheduledMillis,
                                isCompleted = false
                            )
                        )
                    }
                } else {
                    container.reminderRepository.createReminder(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        scheduledAtMillis = scheduledMillis
                    )
                }
                _uiState.update { it.copy(saved = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "خطا: ${e.message}") }
            }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val reminderId: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReminderEditorViewModel(container, reminderId) as T
        }
    }
}

class SearchViewModel(private val container: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results = _query.flatMapLatest { q ->
        container.noteRepository.searchNotes(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(value: String) {
        _query.value = value
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(container) as T
        }
    }
}
