package com.offlinejournal.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.DateFilterPeriod
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.ReportsViewModel
import com.offlinejournal.util.DateRangeHelper
import com.offlinejournal.util.PersianFormatter

@Composable
fun ReportsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { JournalTopBar(title = "گزارش‌ها", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateFilterPeriod.entries.filter { it != DateFilterPeriod.ALL }) { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(DateRangeHelper.labelForPeriod(period)) }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "خلاصه ${DateRangeHelper.labelForPeriod(uiState.selectedPeriod)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "تعداد یادداشت‌ها: ${PersianFormatter.toPersianDigits(uiState.notes.size)}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.notes, key = { it.id }) { note ->
                    ReportNoteItem(note = note, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ReportNoteItem(note: Note, viewModel: ReportsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = note.title ?: "بدون عنوان", style = MaterialTheme.typography.titleMedium)
            Text(
                text = viewModel.formatDate(note.createdAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = note.displayText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}
