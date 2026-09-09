package com.offlinejournal.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.R
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.components.MountainBackground
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.PurpleDark
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.NoteEditorViewModel
import com.offlinejournal.util.PersianFormatter
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun VoiceRecordScreen(
    container: AppContainer,
    categoryId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NoteEditorViewModel = viewModel(
        key = "voice_record_$categoryId",
        factory = NoteEditorViewModel.Factory(container, categoryId = categoryId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val categoryName = uiState.categories.find { it.id == uiState.categoryId }?.name ?: "بدون دسته‌بندی"

    var elapsedSeconds by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeSpeechEngine()
    }

    LaunchedEffect(uiState.savedNoteId) {
        if (uiState.savedNoteId != null) onSaved()
    }

    LaunchedEffect(uiState.isRecording) {
        if (!uiState.isRecording) {
            elapsedSeconds = 0
            return@LaunchedEffect
        }
        elapsedSeconds = 0
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PurplePrimary,
        unfocusedBorderColor = Color(0xFF475569),
        focusedLabelColor = PurplePrimary,
        cursorColor = PurplePrimary,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE2E8F0),
        focusedContainerColor = NavyCard.copy(alpha = 0.6f),
        unfocusedContainerColor = NavyCard.copy(alpha = 0.4f)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MountainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_app_icon),
                        contentDescription = "JayMinder",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, PurplePrimary.copy(alpha = 0.5f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = NavyCard.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "دسته‌بندی: $categoryName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PurplePrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("عنوان (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(0.3f))

                Text(
                    text = when {
                        uiState.isSaving -> "در حال ذخیره..."
                        uiState.isTranscribing -> "در حال تبدیل به متن..."
                        uiState.isRecording -> "در حال ضبط صدا..."
                        uiState.audioFilePath != null -> "ضبط انجام شد"
                        else -> "برای شروع ضبط، دکمه میکروفون را بزنید"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                RecordingCircle(
                    isRecording = uiState.isRecording,
                    elapsedSeconds = elapsedSeconds,
                    onMicClick = {
                        if (uiState.isRecording) return@RecordingCircle
                        val hasMic = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(
                                buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                            )
                        }
                    }
                )

                if (uiState.isRecording) {
                    WaveformBars(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp))
                }

                Spacer(modifier = Modifier.weight(0.5f))

                RecordingBottomControls(
                    isSaving = uiState.isSaving || uiState.isTranscribing,
                    onCancel = {
                        viewModel.cancelRecording()
                        onDismiss()
                    },
                    onConfirm = viewModel::confirmAndSave
                )

                Spacer(modifier = Modifier.height(24.dp))

                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCircle(
    isRecording: Boolean,
    elapsedSeconds: Int,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse2"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(200.dp * pulse2)
                    .clip(CircleShape)
                    .background(PurplePrimary.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(170.dp * pulse1)
                    .clip(CircleShape)
                    .background(PurplePrimary.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary.copy(alpha = 0.18f))
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, PurpleDark)
                    )
                )
                .clickable(onClick = onMicClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "ضبط صدا",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        if (isRecording) {
            Text(
                text = formatTimer(elapsedSeconds),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun WaveformBars(modifier: Modifier = Modifier) {
    val barCount = 24
    val heights = remember { List(barCount) { Random.nextFloat() } }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, base ->
            val animated = (base + ((index + phase * barCount) % barCount) / barCount.toFloat()) % 1f
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = (12 + animated * 28).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PurplePrimary.copy(alpha = 0.5f + animated * 0.5f))
            )
        }
    }
}

@Composable
private fun RecordingBottomControls(
    isSaving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NavyDark.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onCancel)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NavyCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "لغو", tint = Color.White)
                }
                Text(
                    text = "لغو",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = !isSaving, onClick = onConfirm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary, PurpleDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "تأیید", tint = Color.White)
                    }
                }
                Text(
                    text = "تأیید",
                    color = PurplePrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return PersianFormatter.toPersianDigits(
        String.format("%02d:%02d", mins, secs)
    )
}
