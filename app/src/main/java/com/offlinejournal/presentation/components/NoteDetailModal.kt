package com.offlinejournal.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.PurplePrimary

@Composable
fun NoteDetailModal(
    note: Note,
    categoryName: String?,
    durationMs: Long,
    onPlayAudio: (onComplete: () -> Unit) -> Unit,
    onStopAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onStopAudio() }
    }

    Dialog(
        onDismissRequest = {
            onStopAudio()
            onDismiss()
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
                IconButton(
                    onClick = {
                        onStopAudio()
                        onDismiss()
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                }

                DetailField(label = "دسته‌بندی", value = categoryName ?: "بدون دسته‌بندی")
                DetailField(label = "عنوان", value = note.title ?: "بدون عنوان")
                DetailField(
                    label = "جزئیات",
                    value = note.textContent.ifBlank { "—" }
                )
                DetailField(
                    label = "متن تبدیل‌شده از صدا",
                    value = note.transcription?.ifBlank { "—" } ?: "—"
                )

                if (note.audioFilePath != null) {
                    TelegramVoicePlayer(
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        onPlayPause = {
                            if (isPlaying) {
                                onStopAudio()
                                isPlaying = false
                            } else {
                                onPlayAudio { isPlaying = false }
                                isPlaying = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = PurplePrimary,
            fontWeight = FontWeight.Bold
        )
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
