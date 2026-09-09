package com.offlinejournal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinejournal.domain.model.Note
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.PurplePrimary

@Composable
fun TimelineNoteItem(
    note: Note,
    timeText: String,
    categoryName: String?,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                    text = timelineNoteTitle(note),
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
                if (isPlaying) {
                    CompactVoiceWaveform(
                        isPlaying = true,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
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

fun timelineNoteTitle(note: Note): String =
    note.title?.takeIf { it.isNotBlank() } ?: "…"
