package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import com.offlinejournal.util.DateRangeHelper
import com.offlinejournal.util.JalaliCalendar
import com.offlinejournal.util.JalaliDate
import com.offlinejournal.util.PersianFormatter
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class NotesViewTab {
    TODAY,
    WEEK,
    MONTH,
    CUSTOM
}

data class TimelineDayGroup(
    val dayLabel: String,
    val notes: List<Note>
)

data class NotesTimelineUiState(
    val selectedTab: NotesViewTab = NotesViewTab.TODAY,
    val groups: List<TimelineDayGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val headerDate: String = "",
    val customYear: Int = 0,
    val customMonth: Int = 0,
    val customDay: Int = 0
)

class NotesTimelineViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedTab = MutableStateFlow(NotesViewTab.TODAY)
    private val customDate = MutableStateFlow(currentJalali())

    val uiState: StateFlow<NotesTimelineUiState> = combine(
        selectedTab,
        customDate,
        container.noteRepository.observeAllNotes(),
        container.categoryRepository.observeCategories()
    ) { tab, custom, notes, categories ->
        val referenceMillis = customDateToMillis(custom)
        val filtered = filterNotes(notes, tab, referenceMillis)
        val groups = buildGroups(filtered, tab, referenceMillis)
        NotesTimelineUiState(
            selectedTab = tab,
            groups = groups,
            categories = categories,
            headerDate = PersianFormatter.formatJalaliDate(referenceMillis, includeWeekday = true),
            customYear = custom.year,
            customMonth = custom.month,
            customDay = custom.day
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesTimelineUiState())

    fun selectTab(tab: NotesViewTab) {
        selectedTab.value = tab
    }

    fun updateCustomDate(year: Int, month: Int, day: Int) {
        customDate.value = JalaliDate(year, month, day)
    }

    fun formatTime(note: Note): String = PersianFormatter.formatTime(note.createdAtMillis)

    fun playAudio(path: String, onComplete: () -> Unit = {}) {
        container.audioRecorderManager.playAudio(path, onComplete)
    }

    fun stopAudio() {
        container.audioRecorderManager.stopPlayback()
    }

    fun getDurationMs(path: String): Long =
        container.audioRecorderManager.getRecordingDurationMs(path)

    fun categoryName(categories: List<Category>, categoryId: Long?): String? =
        categories.find { it.id == categoryId }?.name

    private fun filterNotes(notes: List<Note>, tab: NotesViewTab, referenceMillis: Long): List<Note> {
        val (start, end) = when (tab) {
            NotesViewTab.TODAY -> DateRangeHelper.rangeForPeriod(
                com.offlinejournal.domain.model.DateFilterPeriod.DAY,
                referenceMillis
            )
            NotesViewTab.WEEK -> DateRangeHelper.rangeForPeriod(
                com.offlinejournal.domain.model.DateFilterPeriod.WEEK,
                referenceMillis
            )
            NotesViewTab.MONTH -> DateRangeHelper.rangeForPeriod(
                com.offlinejournal.domain.model.DateFilterPeriod.MONTH,
                referenceMillis
            )
            NotesViewTab.CUSTOM -> {
                val startOfDay = TehranTime.toZonedDateTime(referenceMillis)
                    .toLocalDate()
                    .atStartOfDay(TehranTime.zoneId)
                    .toInstant()
                    .toEpochMilli()
                startOfDay to startOfDay + 24 * 60 * 60 * 1000
            }
        }
        return notes
            .filter { it.createdAtMillis in start until end }
            .sortedBy { it.createdAtMillis }
    }

    private fun buildGroups(
        notes: List<Note>,
        tab: NotesViewTab,
        referenceMillis: Long
    ): List<TimelineDayGroup> {
        if (notes.isEmpty()) return emptyList()
        if (tab == NotesViewTab.TODAY || tab == NotesViewTab.CUSTOM) {
            return listOf(
                TimelineDayGroup(
                    dayLabel = PersianFormatter.formatJalaliDate(referenceMillis, includeWeekday = true),
                    notes = notes
                )
            )
        }

        return notes
            .groupBy { note ->
                val jalali = JalaliCalendar.fromMillis(note.createdAtMillis)
                "${jalali.year}/${jalali.month}/${jalali.day}"
            }
            .entries
            .sortedBy { entry -> entry.value.first().createdAtMillis }
            .map { (_, dayNotes) ->
                TimelineDayGroup(
                    dayLabel = PersianFormatter.formatJalaliDate(
                        dayNotes.first().createdAtMillis,
                        includeWeekday = true
                    ),
                    notes = dayNotes.sortedBy { it.createdAtMillis }
                )
            }
    }

    private fun customDateToMillis(jalali: JalaliDate): Long {
        val (gy, gm, gd) = JalaliCalendar.toGregorian(jalali)
        return java.time.LocalDate.of(gy, gm, gd)
            .atStartOfDay(TehranTime.zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun currentJalali(): JalaliDate {
        val now = TehranTime.nowMillis()
        return JalaliCalendar.fromMillis(now)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesTimelineViewModel(container) as T
        }
    }
}
