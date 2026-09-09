package com.offlinejournal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.NoteDetailModal
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.NavySurface
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.NotesTimelineViewModel
import com.offlinejournal.presentation.viewmodel.NotesViewTab
import com.offlinejournal.util.JalaliDate

@Composable
fun NotesTimelineScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
    viewModel: NotesTimelineViewModel = viewModel(factory = NotesTimelineViewModel.Factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showCustomDateModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        NotesTabRow(
            selectedTab = uiState.selectedTab,
            onTabSelected = { tab ->
                if (tab == NotesViewTab.CUSTOM) {
                    showCustomDateModal = true
                } else {
                    viewModel.selectTab(tab)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = uiState.headerDate,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (uiState.groups.isEmpty()) {
            EmptyStateMessage("یادداشتی برای این بازه زمانی ندارید.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val showDayHeaders = uiState.selectedTab == NotesViewTab.WEEK ||
                    uiState.selectedTab == NotesViewTab.MONTH ||
                    (uiState.selectedTab == NotesViewTab.CUSTOM && uiState.groups.size > 1)

                uiState.groups.forEach { group ->
                    if (showDayHeaders) {
                        item(key = "header_${group.dayLabel}") {
                            Text(
                                text = group.dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = PurplePrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    itemsIndexed(
                        group.notes,
                        key = { _, note -> note.id }
                    ) { index, note ->
                        TimelineNoteItem(
                            note = note,
                            timeText = viewModel.formatTime(note),
                            categoryName = viewModel.categoryName(uiState.categories, note.categoryId),
                            showTopLine = index > 0,
                            showBottomLine = index < group.notes.lastIndex,
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

    if (showCustomDateModal) {
        CustomDateRangeModal(
            startYear = uiState.customStartYear,
            startMonth = uiState.customStartMonth,
            startDay = uiState.customStartDay,
            endYear = uiState.customEndYear,
            endMonth = uiState.customEndMonth,
            endDay = uiState.customEndDay,
            onDismiss = { showCustomDateModal = false },
            onApply = { start, end ->
                viewModel.applyCustomDateRange(start, end)
                showCustomDateModal = false
            }
        )
    }

    selectedNote?.let { note ->
        NoteDetailModal(
            container = container,
            noteId = note.id,
            onDismiss = { selectedNote = null },
            onDeleted = { selectedNote = null }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    color = if (selected) Color.White else Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CustomDateRangeModal(
    startYear: Int,
    startMonth: Int,
    startDay: Int,
    endYear: Int,
    endMonth: Int,
    endDay: Int,
    onDismiss: () -> Unit,
    onApply: (JalaliDate, JalaliDate) -> Unit
) {
    var sYear by remember { mutableIntStateOf(startYear) }
    var sMonth by remember { mutableIntStateOf(startMonth) }
    var sDay by remember { mutableIntStateOf(startDay) }
    var eYear by remember { mutableIntStateOf(endYear) }
    var eMonth by remember { mutableIntStateOf(endMonth) }
    var eDay by remember { mutableIntStateOf(endDay) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.88f),
                shape = RoundedCornerShape(20.dp),
                color = NavyCard
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "انتخاب بازه تاریخ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "تاریخ ابتدا",
                        style = MaterialTheme.typography.labelLarge,
                        color = PurplePrimary
                    )
                    JalaliDateFields(
                        year = sYear,
                        month = sMonth,
                        day = sDay,
                        onChange = { y, m, d ->
                            sYear = y
                            sMonth = m
                            sDay = d
                        }
                    )

                    Text(
                        text = "تاریخ انتها",
                        style = MaterialTheme.typography.labelLarge,
                        color = PurplePrimary
                    )
                    JalaliDateFields(
                        year = eYear,
                        month = eMonth,
                        day = eDay,
                        onChange = { y, m, d ->
                            eYear = y
                            eMonth = m
                            eDay = d
                        }
                    )

                    Button(
                        onClick = {
                            onApply(JalaliDate(sYear, sMonth, sDay), JalaliDate(eYear, eMonth, eDay))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("اعمال")
                    }
                }
            }
        }
    }
}

@Composable
private fun JalaliDateFields(
    year: Int,
    month: Int,
    day: Int,
    onChange: (Int, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = year.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onChange(it, month, day) } },
            label = { Text("سال") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = month.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onChange(year, it, day) } },
            label = { Text("ماه") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = day.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { onChange(year, month, it) } },
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
    showTopLine: Boolean,
    showBottomLine: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NavyCard)
                .border(1.dp, Color(0xFF2D3A52), RoundedCornerShape(16.dp))
                .padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDetails) {
                Icon(Icons.Default.MoreVert, contentDescription = "جزئیات", tint = Color(0xFF94A3B8))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = noteTitle(note),
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

            if (note.audioFilePath != null) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "پخش",
                        tint = PurplePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TimelineRail(
            timeText = timeText,
            showTopLine = showTopLine,
            showBottomLine = showBottomLine
        )
    }
}

@Composable
private fun TimelineRail(
    timeText: String,
    showTopLine: Boolean,
    showBottomLine: Boolean
) {
    val lineColor = PurplePrimary.copy(alpha = 0.45f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .weight(1f)
                .background(if (showTopLine) lineColor else Color.Transparent)
        )
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
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .weight(1f)
                .background(if (showBottomLine) lineColor else Color.Transparent)
        )
    }
}

private fun noteTitle(note: Note): String =
    note.title?.takeIf { it.isNotBlank() } ?: "…"
