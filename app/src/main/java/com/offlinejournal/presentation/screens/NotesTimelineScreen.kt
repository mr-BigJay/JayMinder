package com.offlinejournal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.NoteDetailModal
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.NavySurface
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.NotesTimelineUiState
import com.offlinejournal.presentation.viewmodel.NotesTimelineViewModel
import com.offlinejournal.presentation.viewmodel.NotesViewTab
@Composable
fun NotesTimelineScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
    viewModel: NotesTimelineViewModel = viewModel(factory = NotesTimelineViewModel.Factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        NotesTabRow(
            selectedTab = uiState.selectedTab,
            onTabSelected = viewModel::selectTab
        )

        if (uiState.selectedTab == NotesViewTab.CUSTOM) {
            CustomDatePicker(
                year = uiState.customYear,
                month = uiState.customMonth,
                day = uiState.customDay,
                onDateChange = viewModel::updateCustomDate
            )
        }

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = uiState.headerDate,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
        }

        if (uiState.groups.isEmpty()) {
            EmptyStateMessage("یادداشتی برای این بازه زمانی ندارید.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.groups.forEach { group ->
                    if (uiState.selectedTab == NotesViewTab.WEEK || uiState.selectedTab == NotesViewTab.MONTH) {
                        item(key = "header_${group.dayLabel}") {
                            Text(
                                text = group.dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = PurplePrimary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(group.notes, key = { it.id }) { note ->
                        TimelineNoteItem(
                            note = note,
                            timeText = viewModel.formatTime(note),
                            categoryName = viewModel.categoryName(uiState.categories, note.categoryId),
                            onPlay = {
                                note.audioFilePath?.let { path ->
                                    viewModel.playAudio(path)
                                }
                            },
                            onDetails = { selectedNote = note }
                        )
                    }
                }
            }
        }
    }

    selectedNote?.let { note ->
        NoteDetailModal(
            note = note,
            categoryName = viewModel.categoryName(uiState.categories, note.categoryId),
            durationMs = note.audioFilePath?.let { viewModel.getDurationMs(it) } ?: 0L,
            onPlayAudio = { onComplete ->
                note.audioFilePath?.let { viewModel.playAudio(it, onComplete) }
            },
            onStopAudio = viewModel::stopAudio,
            onDismiss = { selectedNote = null }
        )
    }
}

@Composable
private fun NotesTabRow(selectedTab: NotesViewTab, onTabSelected: (NotesViewTab) -> Unit) {
    val tabs = listOf(
        NotesViewTab.CUSTOM to "تاریخ مشخص",
        NotesViewTab.MONTH to "ماه",
        NotesViewTab.WEEK to "هفته",
        NotesViewTab.TODAY to "امروز"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val selected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) PurplePrimary else NavySurface
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    color = if (selected) Color.White else Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomDatePicker(
    year: Int,
    month: Int,
    day: Int,
    onDateChange: (Int, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = year.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onDateChange(it, month, day) } },
            label = { Text("سال") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = month.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onDateChange(year, it, day) } },
            label = { Text("ماه") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = day.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onDateChange(year, month, it) } },
            label = { Text("روز") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
private fun TimelineNoteItem(
    note: Note,
    timeText: String,
    categoryName: String?,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineRail(timeText = timeText)

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(NavyCard)
                .border(1.dp, Color(0xFF2D3A52), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.audioFilePath != null) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "پخش", tint = PurplePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title ?: "بدون عنوان",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryName ?: note.displayText.take(40),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurplePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (note.audioFilePath != null) Icons.Default.Mic else Icons.Default.Category,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDetails) {
                Icon(Icons.Default.MoreVert, contentDescription = "جزئیات", tint = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun TimelineRail(timeText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(PurplePrimary)
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = PurplePrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
