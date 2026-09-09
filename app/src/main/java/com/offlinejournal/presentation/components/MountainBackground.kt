package com.offlinejournal.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.PurplePrimary

@Composable
fun MountainBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D1B69),
                            Color(0xFF1A1040),
                            NavyDark
                        )
                    )
                )
        )
        MountainSilhouette(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun MountainSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val backPath = Path().apply {
            moveTo(0f, h * 0.72f)
            lineTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.3f, h * 0.58f)
            lineTo(w * 0.5f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.55f)
            lineTo(w * 0.85f, h * 0.42f)
            lineTo(w, h * 0.65f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(backPath, Color(0xFF0D0820).copy(alpha = 0.6f))

        val frontPath = Path().apply {
            moveTo(0f, h * 0.82f)
            lineTo(w * 0.2f, h * 0.62f)
            lineTo(w * 0.4f, h * 0.75f)
            lineTo(w * 0.6f, h * 0.55f)
            lineTo(w * 0.8f, h * 0.7f)
            lineTo(w, h * 0.78f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(frontPath, Color(0xFF080510).copy(alpha = 0.85f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PurplePrimary.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.38f),
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.38f)
        )
    }
}
