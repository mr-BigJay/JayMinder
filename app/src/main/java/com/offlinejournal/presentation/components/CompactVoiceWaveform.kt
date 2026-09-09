package com.offlinejournal.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.offlinejournal.presentation.theme.PurplePrimary
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CompactVoiceWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 14
) {
    val baseBars = remember(barCount) { List(barCount) { Random.nextInt(6, 16) } }
    val infiniteTransition = rememberInfiniteTransition(label = "compactWave")
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
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        baseBars.forEachIndexed { index, base ->
            val animatedHeight = if (isPlaying) {
                val wave = sin(phase + index * 0.5f).toFloat()
                (base + wave * 8).coerceIn(4f, 20f)
            } else {
                base.toFloat()
            }
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = animatedHeight.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isPlaying) PurplePrimary.copy(alpha = 0.9f)
                        else PurplePrimary.copy(alpha = 0.4f)
                    )
            )
        }
    }
}
