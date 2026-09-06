package com.offlinejournal.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.DateFilterPeriod
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onNewNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onCategories: () -> Unit,
    onReminders: () -> Unit,
    onReports: () -> Unit,
    onSearch: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JournalTopBar(
                title = "یادداشت‌های من",
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                    IconButton(onClick = onCategories) {
                        Icon(Icons.Default.Category, contentDescription = "دسته‌بندی")
                    }
                    IconButton(onClick = onReminders) {
                        Icon(Icons.Default.Notifications, contentDescription = "یادآوری")
                    }
                    IconButton(onClick = onReports) {
                        Icon(Icons.Default.Assessment, contentDescription = "گزارش‌ها")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, contentDescription = "یادداشت جدید")
            }
        }
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
                items(DateFilterPeriod.entries) { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.setPeriod(period) },
                        label = {
                            Text(
                                when (period) {
                                    DateFilterPeriod.DAY -> "امروز"
                                    DateFilterPeriod.WEEK -> "این هفته"
                                    DateFilterPeriod.MONTH -> "این ماه"
                                    DateFilterPeriod.YEAR -> "امسال"
                                    DateFilterPeriod.ALL -> "همه"
                                }
                            )
                        }
                    )
                }
            }

            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryId == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("همه دسته‌ها") }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { viewModel.setCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            if (uiState.notes.isEmpty()) {
                EmptyStateMessage(
                    message = "هنوز یادداشتی ندارید. با دکمه + یادداشت جدید بسازید.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            dateText = viewModel.formatNoteDate(note),
                            categoryName = uiState.categories.find { it.id == note.categoryId }?.name,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    dateText: String,
    categoryName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title ?: "بدون عنوان",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                }
            }
            Text(
                text = note.displayText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (note.audioFilePath != null) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "صوتی",
                        modifier = Modifier.padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                categoryName?.let {
                    Text(
                        text = "• $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
