package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.DateFilterPeriod
import com.offlinejournal.domain.model.Note
import com.offlinejournal.domain.model.Reminder
import com.offlinejournal.util.DateRangeHelper
import com.offlinejournal.util.PersianFormatter
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val todayReminders: List<Reminder> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.ALL,
    val selectedCategoryId: Long? = null,
    val greetingDate: String = "",
    val currentTime: String = "",
    val playingNoteId: Long? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(DateFilterPeriod.ALL)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val playingNoteId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        selectedPeriod,
        selectedCategoryId,
        container.categoryRepository.observeCategories(),
        container.reminderRepository.observeUpcomingReminders()
    ) { period, categoryId, categories, upcomingReminders ->
        DashboardInputs(period, categoryId, categories, upcomingReminders)
    }.flatMapLatest { inputs ->
        container.noteRepository.observeNotesByPeriod(inputs.period, inputs.categoryId)
            .combine(selectedPeriod) { notes, period ->
                notes to period
            }
            .combine(playingNoteId) { (notes, period), playingId ->
                val now = TehranTime.nowMillis()
                val todayReminders = inputs.upcomingReminders
                    .filter { DateRangeHelper.isToday(it.scheduledAtMillis, now) }
                    .sortedBy { it.scheduledAtMillis }
                HomeUiState(
                    notes = notes,
                    recentNotes = notes.take(3),
                    todayReminders = todayReminders,
                    categories = inputs.categories,
                    selectedPeriod = period,
                    selectedCategoryId = inputs.categoryId,
                    greetingDate = PersianFormatter.formatJalaliDate(now, includeWeekday = true),
                    currentTime = PersianFormatter.formatTime(now),
                    playingNoteId = playingId,
                    isLoading = false
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setPeriod(period: DateFilterPeriod) {
        selectedPeriod.value = period
    }

    fun setCategory(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            container.audioRecorderManager.deleteAudioFile(note.audioFilePath)
            container.noteRepository.deleteNote(note)
        }
    }

    fun formatNoteDate(note: Note): String {
        return when {
            DateRangeHelper.isToday(note.createdAtMillis) -> "امروز، ${PersianFormatter.formatTime(note.createdAtMillis)}"
            DateRangeHelper.isYesterday(note.createdAtMillis) -> "دیروز، ${PersianFormatter.formatTime(note.createdAtMillis)}"
            else -> PersianFormatter.formatDateTime(note.createdAtMillis)
        }
    }

    fun formatReminderTime(reminder: Reminder): String =
        PersianFormatter.formatTime(reminder.scheduledAtMillis)

    fun formatReminderRelative(reminder: Reminder): String? =
        PersianFormatter.formatRelativeUntil(reminder.scheduledAtMillis)

    fun formatNoteTime(note: Note): String =
        PersianFormatter.formatTime(note.createdAtMillis)

    fun categoryName(categoryId: Long?): String? =
        uiState.value.categories.find { it.id == categoryId }?.name

    fun toggleNotePlayback(note: Note) {
        val path = note.audioFilePath ?: return
        if (playingNoteId.value == note.id) {
            stopAudio()
            return
        }
        stopAudio()
        playingNoteId.value = note.id
        container.audioRecorderManager.playAudio(path) {
            playingNoteId.value = null
        }
    }

    fun stopAudio() {
        container.audioRecorderManager.stopPlayback()
        playingNoteId.value = null
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(container) as T
        }
    }

    private data class DashboardInputs(
        val period: DateFilterPeriod,
        val categoryId: Long?,
        val categories: List<Category>,
        val upcomingReminders: List<Reminder>
    )
}
