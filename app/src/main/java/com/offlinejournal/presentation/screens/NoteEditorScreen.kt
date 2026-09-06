package com.offlinejournal.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.NoteEditorViewModel
import com.offlinejournal.service.speech.SpeechEngineState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    container: AppContainer,
    noteId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NoteEditorViewModel = viewModel(
        factory = NoteEditorViewModel.Factory(container, noteId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var categoryExpanded by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            JournalTopBar(
                title = if (noteId == null) "یادداشت جدید" else "ویرایش یادداشت",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("عنوان (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.textContent,
                onValueChange = viewModel::updateText,
                label = { Text("متن یادداشت") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Text(
                text = "ضبط صدا",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isRecording) {
                    FilledTonalButton(onClick = viewModel::stopRecording) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Text("توقف ضبط", modifier = Modifier.padding(start = 4.dp))
                    }
                    TextButton(onClick = viewModel::cancelRecording) {
                        Text("لغو")
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
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
                            CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
                        } else {
                            Icon(Icons.Default.Translate, contentDescription = null)
                        }
                        Text("تبدیل به متن", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            when (val speechState = uiState.speechState) {
                is SpeechEngineState.Initializing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("در حال بارگذاری مدل تشخیص گفتار...")
                    }
                }
                is SpeechEngineState.Error -> {
                    Text(
                        text = speechState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }

            if (uiState.transcription.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.transcription,
                    onValueChange = viewModel::updateTranscription,
                    label = { Text("متن تبدیل‌شده از صدا") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            if (uiState.categories.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.categories.find { it.id == uiState.categoryId }?.name ?: "بدون دسته‌بندی",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("دسته‌بندی") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون دسته‌بندی") },
                            onClick = {
                                viewModel.selectCategory(null)
                                categoryExpanded = false
                            }
                        )
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.selectCategory(category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::saveNote,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("ذخیره یادداشت")
            }
        }
    }
}
