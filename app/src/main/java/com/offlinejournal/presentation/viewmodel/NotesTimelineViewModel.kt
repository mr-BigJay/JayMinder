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
    val playingNoteId: Long? = null,
    val headerDate: String = "",
    val customStartYear: Int = 0,
    val customStartMonth: Int = 0,
    val customStartDay: Int = 0,
    val customEndYear: Int = 0,
    val customEndMonth: Int = 0,
    val customEndDay: Int = 0
)

class NotesTimelineViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedTab = MutableStateFlow(NotesViewTab.TODAY)
    private val customStartDate = MutableStateFlow(currentJalali())
    private val customEndDate = MutableStateFlow(currentJalali())
    private val playingNoteId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<NotesTimelineUiState> = combine(
        selectedTab,
        customStartDate,
        customEndDate,
        container.noteRepository.observeAllNotes(),
        container.categoryRepository.observeCategories()
    ) { tab, start, end, notes, categories ->
        val filtered = filterNotes(notes, tab, start, end)
        val groups = buildGroups(filtered, tab, start, end)
        NotesTimelineUiState(
            selectedTab = tab,
            groups = groups,
            categories = categories,
            headerDate = headerForTab(tab, start, end),
            customStartYear = start.year,
            customStartMonth = start.month,
            customStartDay = start.day,
            customEndYear = end.year,
            customEndMonth = end.month,
            customEndDay = end.day
        )
    }.combine(playingNoteId) { state, playingId ->
        state.copy(playingNoteId = playingId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesTimelineUiState())

    fun selectTab(tab: NotesViewTab) {
        selectedTab.value = tab
    }

    fun applyCustomDateRange(start: JalaliDate, end: JalaliDate) {
        val normalizedStart = start
        val normalizedEnd = if (jalaliToMillis(end) < jalaliToMillis(start)) start else end
        customStartDate.value = normalizedStart
        customEndDate.value = normalizedEnd
        selectedTab.value = NotesViewTab.CUSTOM
    }

    fun formatTime(note: Note): String = PersianFormatter.formatTime(note.createdAtMillis)

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

    fun getDurationMs(path: String): Long =
        container.audioRecorderManager.getRecordingDurationMs(path)

    fun categoryName(categories: List<Category>, categoryId: Long?): String? =
        categories.find { it.id == categoryId }?.name

    private fun filterNotes(
        notes: List<Note>,
        tab: NotesViewTab,
        start: JalaliDate,
        end: JalaliDate
    ): List<Note> {
        val (rangeStart, rangeEnd) = when (tab) {
            NotesViewTab.TODAY -> {
                val ref = TehranTime.nowMillis()
                DateRangeHelper.rangeForPeriod(
                    com.offlinejournal.domain.model.DateFilterPeriod.DAY,
                    ref
                )
            }
            NotesViewTab.WEEK -> {
                val ref = TehranTime.nowMillis()
                DateRangeHelper.rangeForPeriod(
                    com.offlinejournal.domain.model.DateFilterPeriod.WEEK,
                    ref
                )
            }
            NotesViewTab.MONTH -> {
                val ref = TehranTime.nowMillis()
                DateRangeHelper.rangeForPeriod(
                    com.offlinejournal.domain.model.DateFilterPeriod.MONTH,
                    ref
                )
            }
            NotesViewTab.CUSTOM -> {
                jalaliStartOfDayMillis(start) to jalaliEndOfDayMillis(end)
            }
        }
        return notes
            .filter { it.createdAtMillis in rangeStart until rangeEnd }
            .sortedBy { it.createdAtMillis }
    }

    private fun buildGroups(
        notes: List<Note>,
        tab: NotesViewTab,
        start: JalaliDate,
        end: JalaliDate
    ): List<TimelineDayGroup> {
        if (notes.isEmpty()) return emptyList()
        if (tab == NotesViewTab.TODAY) {
            val ref = TehranTime.nowMillis()
            return listOf(
                TimelineDayGroup(
                    dayLabel = PersianFormatter.formatJalaliDate(ref, includeWeekday = true),
                    notes = notes
                )
            )
        }
        if (tab == NotesViewTab.CUSTOM && jalaliToMillis(start) == jalaliToMillis(end)) {
            return listOf(
                TimelineDayGroup(
                    dayLabel = PersianFormatter.formatJalaliDate(jalaliStartOfDayMillis(start), includeWeekday = true),
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

    private fun headerForTab(tab: NotesViewTab, start: JalaliDate, end: JalaliDate): String {
        return when (tab) {
            NotesViewTab.TODAY -> PersianFormatter.formatJalaliDate(TehranTime.nowMillis(), includeWeekday = true)
            NotesViewTab.WEEK -> "این هفته"
            NotesViewTab.MONTH -> "این ماه"
            NotesViewTab.CUSTOM -> {
                val startText = PersianFormatter.formatJalaliDate(jalaliStartOfDayMillis(start), includeWeekday = true)
                val endText = PersianFormatter.formatJalaliDate(jalaliStartOfDayMillis(end), includeWeekday = true)
                if (startText == endText) startText else "از $startText تا $endText"
            }
        }
    }

    private fun jalaliStartOfDayMillis(jalali: JalaliDate): Long {
        val (gy, gm, gd) = JalaliCalendar.toGregorian(jalali)
        return java.time.LocalDate.of(gy, gm, gd)
            .atStartOfDay(TehranTime.zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun jalaliEndOfDayMillis(jalali: JalaliDate): Long =
        jalaliStartOfDayMillis(jalali) + 24 * 60 * 60 * 1000

    private fun jalaliToMillis(jalali: JalaliDate): Long = jalaliStartOfDayMillis(jalali)

    private fun currentJalali(): JalaliDate = JalaliCalendar.fromMillis(TehranTime.nowMillis())

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesTimelineViewModel(container) as T
        }
    }
}
