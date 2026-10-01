package com.offlinejournal.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.NoteEditorViewModel
import com.offlinejournal.service.ai.TranscriptionPipelineMode
import com.offlinejournal.service.ai.TranscriptionPipelineStatus
import com.offlinejournal.service.speech.ModelInstallState
import com.offlinejournal.service.speech.SpeechEngineState

@Composable
fun NoteEditorScreen(
    container: AppContainer,
    categoryId: Long? = null,
    noteId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NoteEditorViewModel = viewModel(
        factory = NoteEditorViewModel.Factory(container, noteId, categoryId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val categoryName = uiState.categories.find { it.id == uiState.categoryId }?.name ?: "بدون دسته‌بندی"
    var apiKeyDraft by rememberSaveable { mutableStateOf("") }

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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Scaffold(
        topBar = {
            JournalTopBar(
                title = if (noteId == null) "یادداشت جدید" else "ویرایش یادداشت",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("بازگشت")
                }
                Button(
                    onClick = viewModel::saveNote,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 6.dp))
                    }
                    Text("ذخیره")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "دسته‌بندی: $categoryName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("عنوان یادداشت") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors
            )

            OutlinedTextField(
                value = uiState.textContent,
                onValueChange = viewModel::updateText,
                label = { Text("یادداشت") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                colors = fieldColors
            )

            Text(text = "ضبط صدا", style = MaterialTheme.typography.titleMedium)

            Text(text = "موتور تبدیل", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.transcriptionMode == TranscriptionPipelineMode.OFFLINE,
                    onClick = { viewModel.setTranscriptionMode(TranscriptionPipelineMode.OFFLINE) },
                    label = { Text("Offline") }
                )
                FilterChip(
                    selected = uiState.transcriptionMode == TranscriptionPipelineMode.AI,
                    onClick = { viewModel.setTranscriptionMode(TranscriptionPipelineMode.AI) },
                    label = { Text("AI") }
                )
            }
            Text(
                text = when (uiState.transcriptionMode) {
                    TranscriptionPipelineMode.OFFLINE -> "Vosk → ذخیره"
                    TranscriptionPipelineMode.AI -> "Vosk → ArvanCloud → ذخیره"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.transcriptionMode == TranscriptionPipelineMode.AI && !uiState.aiApiKeyConfigured) {
                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    label = { Text("کلید API ArvanCloud") },
                    supportingText = {
                        Text("فقط روی دستگاه ذخیره می‌شود؛ در Git یا APK commit نمی‌شود.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors
                )
                TextButton(
                    onClick = {
                        viewModel.saveAiApiKey(apiKeyDraft)
                        apiKeyDraft = ""
                    },
                    enabled = apiKeyDraft.isNotBlank()
                ) {
                    Text("ذخیره کلید API")
                }
            }

            when (val pipeline = uiState.pipelineStatus) {
                TranscriptionPipelineStatus.ConvertingSpeech -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        Text("در حال تبدیل صدا...")
                    }
                }
                TranscriptionPipelineStatus.ImprovingText -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        Text("در حال بهبود متن...")
                    }
                }
                TranscriptionPipelineStatus.Completed -> {
                    Text(
                        "تکمیل شد",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is TranscriptionPipelineStatus.SmartCleanupUnavailable -> {
                    Text(
                        pipeline.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TranscriptionPipelineStatus.Idle -> Unit
            }

            when (val modelState = uiState.modelState) {
                is ModelInstallState.Installing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        Text(modelState.message)
                    }
                }
                is ModelInstallState.Error -> {
                    Text(text = modelState.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isRecording) {
                    FilledTonalButton(onClick = viewModel::stopRecording) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Text("توقف", modifier = Modifier.padding(start = 4.dp))
                    }
                    TextButton(onClick = viewModel::cancelRecording) { Text("لغو") }
                } else {
                    FilledTonalButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) viewModel.startRecording()
                            else permissionLauncher.launch(
                                buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                            )
                        }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Text("ضبط صدا", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                if (uiState.audioFilePath != null && !uiState.isRecording) {
                    FilledTonalButton(onClick = viewModel::playRecording) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("پخش", modifier = Modifier.padding(start = 4.dp))
                    }
                    FilledTonalButton(
                        onClick = viewModel::transcribeAudio,
                        enabled = !uiState.isTranscribing
                    ) {
                        if (uiState.isTranscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Translate, contentDescription = null)
                        }
                        Text("تبدیل به متن", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            when (val speechState = uiState.speechState) {
                is SpeechEngineState.Initializing -> {
                    Text("در حال بارگذاری مدل…")
                }
                is SpeechEngineState.Transcribing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("در حال تبدیل صدا به متن…")
                    }
                }
                is SpeechEngineState.Error -> {
                    Text(text = speechState.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }

            if (uiState.isRecording && uiState.transcription.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.transcription,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("متن زنده") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = fieldColors
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = uiState.transcription,
                onValueChange = viewModel::updateTranscription,
                label = { Text("متن تبدیل‌شده از صدا") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                colors = fieldColors
            )

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
