package com.offlinejournal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.PurpleDark
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.NoteDetailModalViewModel

@Composable
fun NoteDetailModal(
    container: AppContainer,
    noteId: Long,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: NoteDetailModalViewModel = viewModel(
        factory = NoteDetailModalViewModel.Factory(container, noteId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PurplePrimary,
        unfocusedBorderColor = Color(0xFF475569),
        focusedTextColor = Color(0xFFE2E8F0),
        unfocusedTextColor = Color(0xFFE2E8F0),
        cursorColor = PurplePrimary,
        focusedContainerColor = NavyDark,
        unfocusedContainerColor = NavyDark
    )

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAudio() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف یادداشت") },
            text = { Text("آیا از حذف این یادداشت مطمئن هستید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteNote { onDeleted() }
                    }
                ) {
                    Text("بله، حذف شود", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("ذخیره تغییرات") },
            text = { Text("آیا از ذخیره تغییرات مطمئن هستید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirm = false
                        viewModel.stopAudio()
                        viewModel.saveChanges(onDismiss)
                    }
                ) {
                    Text("بله، ذخیره شود", color = PurplePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = {
            viewModel.stopAudio()
            if (!uiState.hasChanges) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = NavyCard
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.hasChanges) {
                        IconButton(
                            onClick = { showSaveConfirm = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PurplePrimary, PurpleDark)
                                    )
                                )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "ذخیره تغییرات",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.stopAudio()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "منو", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("حذف یادداشت", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }

                ReadOnlyField(label = "دسته‌بندی", value = uiState.categoryName)

                EditableField(
                    label = "عنوان",
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    colors = fieldColors,
                    singleLine = true
                )

                EditableField(
                    label = "جزئیات",
                    value = uiState.textContent,
                    onValueChange = viewModel::updateTextContent,
                    colors = fieldColors,
                    minLines = 2
                )

                TranscriptionField(
                    label = "متن تبدیل‌شده از صدا",
                    value = uiState.transcription,
                    onValueChange = viewModel::updateTranscription,
                    colors = fieldColors,
                    isTranscribing = uiState.isTranscribing,
                    onClear = viewModel::clearTranscription,
                    onRegenerate = viewModel::regenerateTranscription
                )

                uiState.note?.audioFilePath?.let {
                    TelegramVoicePlayer(
                        durationMs = uiState.durationMs,
                        isPlaying = isPlaying,
                        onPlayPause = {
                            if (isPlaying) {
                                viewModel.stopAudio()
                                isPlaying = false
                            } else {
                                viewModel.playAudio { isPlaying = false }
                                isPlaying = true
                            }
                        }
                    )
                }

                uiState.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = PurplePrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = NavyDark
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = minLines
        )
    }
}

@Composable
private fun TranscriptionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    isTranscribing: Boolean,
    onClear: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FieldLabel(label)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onRegenerate,
                    enabled = !isTranscribing
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp,
                            color = PurplePrimary
                        )
                    } else {
                        Text(text = "🔄", fontSize = 20.sp)
                    }
                }
                IconButton(onClick = onClear) {
                    Text(text = "🗑️", fontSize = 20.sp)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = PurplePrimary,
        fontWeight = FontWeight.Bold
    )
}
