package com.offlinejournal.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.HomeViewModel

@Composable
fun NotesListScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
) {
    val homeState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { JournalTopBar(title = "یادداشت‌ها", onBack = onBack) }
    ) { padding ->
        if (homeState.notes.isEmpty()) {
            EmptyStateMessage(
                message = "هنوز یادداشتی ندارید.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(homeState.notes, key = { it.id }) { note ->
                    NoteListCard(
                        note = note,
                        categoryName = homeState.categories.find { it.id == note.categoryId }?.name,
                        dateText = viewModel.formatNoteDate(note),
                        onClick = { onNoteClick(note.id) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteListCard(
    note: Note,
    categoryName: String?,
    dateText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title ?: "بدون عنوان",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.displayText.isNotBlank()) {
                Text(
                    text = note.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = PurplePrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.audioFilePath != null) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    categoryName?.let {
                        Text(
                            text = " • $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA78BFA)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
