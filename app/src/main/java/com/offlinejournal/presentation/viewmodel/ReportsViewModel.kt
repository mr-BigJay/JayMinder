package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.DateFilterPeriod
import com.offlinejournal.domain.model.Note
import com.offlinejournal.util.PersianFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ReportSummary(
    val period: DateFilterPeriod,
    val label: String,
    val noteCount: Int
)

data class ReportsUiState(
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.MONTH,
    val notes: List<Note> = emptyList(),
    val summaries: List<ReportSummary> = emptyList()
)

class ReportsViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(DateFilterPeriod.MONTH)

    val uiState: StateFlow<ReportsUiState> = selectedPeriod
        .flatMapLatest { period ->
            combine(
                container.noteRepository.observeNotesByPeriod(period),
                selectedPeriod
            ) { notes, selected ->
                ReportsUiState(
                    selectedPeriod = selected,
                    notes = notes,
                    summaries = emptyList()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun setPeriod(period: DateFilterPeriod) {
        selectedPeriod.value = period
    }

    fun formatDate(millis: Long): String = PersianFormatter.formatJalaliDate(millis)

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(container) as T
        }
    }
}
