package com.offlinejournal.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.PurpleDark
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.util.PersianFormatter
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun TelegramVoicePlayer(
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseBars = remember { List(28) { Random.nextInt(8, 24) } }
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyDark)
            .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatDuration(durationMs),
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            baseBars.forEachIndexed { index, base ->
                val animatedHeight = if (isPlaying) {
                    val wave = sin(phase + index * 0.45f).toFloat()
                    (base + wave * 10).coerceIn(6f, 28f)
                } else {
                    base.toFloat()
                }
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = animatedHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isPlaying) PurplePrimary.copy(alpha = 0.85f)
                            else PurplePrimary.copy(alpha = 0.45f)
                        )
                )
            }
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, PurpleDark)
                    )
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "توقف" else "پخش",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return PersianFormatter.toPersianDigits(String.format("%d:%02d", minutes, seconds))
}
