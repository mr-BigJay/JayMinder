package com.offlinejournal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinejournal.util.PersianFormatter
import kotlin.random.Random

@Composable
fun TelegramVoicePlayer(
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bars = rememberVoiceBars()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF4A6572))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF5DADE2))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "توقف" else "پخش",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEach { height ->
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFB0BEC5).copy(alpha = if (isPlaying) 1f else 0.7f))
                )
            }
        }

        Text(
            text = formatDuration(durationMs),
            color = Color(0xFFECEFF1),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun rememberVoiceBars(): List<Int> {
    return androidx.compose.runtime.remember {
        List(28) { Random.nextInt(8, 28) }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return PersianFormatter.toPersianDigits(String.format("%d:%02d", minutes, seconds))
}
