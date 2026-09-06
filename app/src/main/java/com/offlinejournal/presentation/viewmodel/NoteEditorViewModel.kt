package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import com.offlinejournal.service.speech.SpeechEngineState
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class NoteEditorUiState(
    val title: String = "",
    val textContent: String = "",
    val transcription: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isRecording: Boolean = false,
    val audioFilePath: String? = null,
    val isTranscribing: Boolean = false,
    val speechState: SpeechEngineState = SpeechEngineState.NotInitialized,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedNoteId: Long? = null
)

class NoteEditorViewModel(
    private val container: AppContainer,
    private val existingNoteId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        viewModelScope.launch {
            container.speechToTextEngine.state.collect { state ->
                _uiState.update {
                    it.copy(
                        speechState = state,
                        isTranscribing = state is SpeechEngineState.Transcribing
                    )
                }
            }
        }

        if (existingNoteId != null) {
            loadNote(existingNoteId)
        }
    }

    private fun loadNote(noteId: Long) {
        viewModelScope.launch {
            container.noteRepository.getNote(noteId)?.let { note ->
                _uiState.update {
                    it.copy(
                        title = note.title ?: "",
                        textContent = note.textContent,
                        transcription = note.transcription ?: "",
                        categoryId = note.categoryId,
                        audioFilePath = note.audioFilePath
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun updateText(value: String) = _uiState.update { it.copy(textContent = value) }
    fun updateTranscription(value: String) = _uiState.update { it.copy(transcription = value) }
    fun selectCategory(categoryId: Long?) = _uiState.update { it.copy(categoryId = categoryId) }

    fun startRecording() {
        val result = container.audioRecorderManager.startRecording()
        if (result.isSuccess) {
            _uiState.update { it.copy(isRecording = true, errorMessage = null) }
        } else {
            _uiState.update {
                it.copy(errorMessage = "خطا در شروع ضبط صدا: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun stopRecording() {
        val file = container.audioRecorderManager.stopRecording()
        _uiState.update {
            it.copy(
                isRecording = false,
                audioFilePath = file?.absolutePath
            )
        }
    }

    fun cancelRecording() {
        container.audioRecorderManager.cancelRecording()
        _uiState.update { it.copy(isRecording = false, audioFilePath = null) }
    }

    fun playRecording() {
        val path = _uiState.value.audioFilePath ?: return
        container.audioRecorderManager.playAudio(path)
    }

    fun transcribeAudio() {
        val path = _uiState.value.audioFilePath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribing = true, errorMessage = null) }
            val result = container.speechToTextEngine.transcribeFile(path)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        transcription = result.getOrDefault(""),
                        isTranscribing = false
                    )
                } else {
                    it.copy(
                        isTranscribing = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "تبدیل صدا به متن ناموفق بود"
                    )
                }
            }
        }
    }

    fun saveNote() {
        val state = _uiState.value
        val content = state.transcription.ifBlank { state.textContent }
        if (content.isBlank() && state.audioFilePath == null) {
            _uiState.update { it.copy(errorMessage = "یادداشت نمی‌تواند خالی باشد") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                if (existingNoteId != null) {
                    val existing = container.noteRepository.getNote(existingNoteId)
                    if (existing != null) {
                        container.noteRepository.updateNote(
                            existing.copy(
                                title = state.title.takeIf { it.isNotBlank() },
                                textContent = state.textContent,
                                transcription = state.transcription.takeIf { it.isNotBlank() },
                                categoryId = state.categoryId,
                                audioFilePath = state.audioFilePath
                            )
                        )
                        _uiState.update { it.copy(isSaving = false, savedNoteId = existingNoteId) }
                    }
                } else {
                    val id = container.noteRepository.createNote(
                        textContent = state.textContent,
                        title = state.title.takeIf { it.isNotBlank() },
                        audioFilePath = state.audioFilePath,
                        transcription = state.transcription.takeIf { it.isNotBlank() },
                        categoryId = state.categoryId
                    )
                    _uiState.update { it.copy(isSaving = false, savedNoteId = id) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "خطا در ذخیره: ${e.message}")
                }
            }
        }
    }

    fun initializeSpeechEngine() {
        viewModelScope.launch {
            container.speechToTextEngine.initialize()
        }
    }

    override fun onCleared() {
        container.audioRecorderManager.stopPlayback()
        super.onCleared()
    }

    class Factory(
        private val container: AppContainer,
        private val noteId: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(container, noteId) as T
        }
    }
}
