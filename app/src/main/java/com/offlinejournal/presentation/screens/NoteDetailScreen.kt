package com.offlinejournal.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.NoteDetailViewModel
import com.offlinejournal.util.PersianFormatter

@Composable
fun NoteDetailScreen(
    container: AppContainer,
    noteId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: NoteDetailViewModel = viewModel(
        factory = NoteDetailViewModel.Factory(container, noteId)
    )
) {
    val note by viewModel.note.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            JournalTopBar(
                title = "جزئیات یادداشت",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                    }
                }
            )
        }
    ) { padding ->
        note?.let { n ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = n.title ?: "بدون عنوان",
                    style = MaterialTheme.typography.headlineMedium
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تاریخ ایجاد",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = PersianFormatter.formatDateTime(n.createdAtMillis),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = PersianFormatter.formatJalaliDateNumeric(n.createdAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                categories.find { it.id == n.categoryId }?.let { cat ->
                    Text(
                        text = "دسته‌بندی: ${cat.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (n.textContent.isNotBlank()) {
                    Text(text = "متن:", style = MaterialTheme.typography.titleMedium)
                    Text(text = n.textContent, style = MaterialTheme.typography.bodyLarge)
                }

                if (!n.transcription.isNullOrBlank()) {
                    Text(text = "متن صوتی:", style = MaterialTheme.typography.titleMedium)
                    Text(text = n.transcription, style = MaterialTheme.typography.bodyLarge)
                }

                if (n.audioFilePath != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.playAudio(n.audioFilePath) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("پخش ضبط", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Button(
                    onClick = { viewModel.deleteNote(n, onDeleted) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("حذف یادداشت", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
