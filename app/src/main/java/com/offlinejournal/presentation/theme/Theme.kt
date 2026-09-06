package com.offlinejournal.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.offlinejournal.R

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

val NavyDark = Color(0xFF0A0E1A)
val NavySurface = Color(0xFF121B2E)
val NavyCard = Color(0xFF1A2438)
val PurplePrimary = Color(0xFF8B5CF6)
val PurpleLight = Color(0xFFA78BFA)
val PurpleDark = Color(0xFF6D28D9)

private val JayMinderColors = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    onSecondary = NavyDark,
    tertiary = Color(0xFFC4B5FD),
    background = NavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = NavySurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = NavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val PersianTypography = Typography(
    displayLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    displayMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun OfflineJournalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JayMinderColors,
        typography = PersianTypography,
        content = content
    )
}
