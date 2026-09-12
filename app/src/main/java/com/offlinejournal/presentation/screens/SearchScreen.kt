package com.offlinejournal.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.SearchViewModel
import com.offlinejournal.util.PersianFormatter

@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory(container))
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = { JournalTopBar(title = "جستجو", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateQuery,
                label = { Text("جستجو در یادداشت‌ها...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (query.isBlank()) {
                EmptyStateMessage("عبارت مورد نظر را وارد کنید.")
            } else if (results.isEmpty()) {
                EmptyStateMessage("نتیجه‌ای پیدا نشد.")
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { note ->
                        SearchResultCard(note = note, onClick = { onNoteClick(note.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title ?: "بدون عنوان",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = note.displayText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = PersianFormatter.formatDateTime(note.createdAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
