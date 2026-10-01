package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteDetailModalUiState(
    val note: Note? = null,
    val title: String = "",
    val textContent: String = "",
    val transcription: String = "",
    val categoryName: String = "",
    val durationMs: Long = 0L,
    val isTranscribing: Boolean = false,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val errorMessage: String? = null
)

class NoteDetailModalViewModel(
    private val container: AppContainer,
    private val noteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailModalUiState())
    val uiState: StateFlow<NoteDetailModalUiState> = _uiState.asStateFlow()

    private var categories: List<Category> = emptyList()
    private var fieldsLoaded = false
    private var originalTitle = ""
    private var originalTextContent = ""
    private var originalTranscription = ""

    init {
        viewModelScope.launch {
            container.categoryRepository.observeCategories().collect { cats ->
                categories = cats
                refreshCategoryName()
            }
        }
        viewModelScope.launch {
            container.noteRepository.observeNote(noteId).collect { note ->
                if (note != null && !fieldsLoaded) {
                    fieldsLoaded = true
                    originalTitle = note.title ?: ""
                    originalTextContent = note.textContent
                    originalTranscription = note.transcription ?: ""
                    _uiState.update {
                        it.copy(
                            note = note,
                            title = originalTitle,
                            textContent = originalTextContent,
                            transcription = originalTranscription,
                            categoryName = categoryName(note.categoryId),
                            durationMs = note.audioFilePath?.let { path ->
                                container.audioRecorderManager.getRecordingDurationMs(path)
                            } ?: 0L,
                            hasChanges = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            note = note,
                            categoryName = categoryName(note?.categoryId),
                            durationMs = note?.audioFilePath?.let { path ->
                                container.audioRecorderManager.getRecordingDurationMs(path)
                            } ?: it.durationMs
                        )
                    }
                }
            }
        }
    }

    private fun refreshCategoryName() {
        val note = _uiState.value.note
        if (note != null) {
            _uiState.update { it.copy(categoryName = categoryName(note.categoryId)) }
        }
    }

    private fun categoryName(categoryId: Long?): String =
        categories.find { it.id == categoryId }?.name ?: "بدون دسته‌بندی"

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, hasChanges = computeHasChanges(value, it.textContent, it.transcription)) }
    fun updateTextContent(value: String) = _uiState.update { it.copy(textContent = value, hasChanges = computeHasChanges(it.title, value, it.transcription)) }
    fun updateTranscription(value: String) = _uiState.update { it.copy(transcription = value, hasChanges = computeHasChanges(it.title, it.textContent, value)) }
    fun clearTranscription() = updateTranscription("")

    private fun computeHasChanges(title: String, textContent: String, transcription: String): Boolean =
        title != originalTitle ||
            textContent != originalTextContent ||
            transcription != originalTranscription

    fun regenerateTranscription() {
        val path = _uiState.value.note?.audioFilePath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribing = true, errorMessage = null) }
            val modelResult = container.speechToTextEngine.ensureModelInstalled()
            if (modelResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        errorMessage = modelResult.exceptionOrNull()?.message ?: "مدل آماده نیست"
                    )
                }
                return@launch
            }
            val result = container.speechToTextEngine.transcribeFile(path)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "تبدیل ناموفق بود"
                    )
                }
                return@launch
            }
            val raw = result.getOrDefault("")
            val (finalText, cleanupError) = container.transcriptionPipelineCoordinator
                .finalizeTranscription(raw) { }
            _uiState.update {
                it.copy(
                    transcription = finalText,
                    isTranscribing = false,
                    errorMessage = cleanupError,
                    hasChanges = computeHasChanges(it.title, it.textContent, finalText)
                )
            }
        }
    }

    fun saveChanges(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val note = state.note ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                container.noteRepository.updateNote(
                    note.copy(
                        title = state.title.takeIf { it.isNotBlank() },
                        textContent = state.textContent,
                        transcription = state.transcription.takeIf { it.isNotBlank() }
                    )
                )
                originalTitle = state.title
                originalTextContent = state.textContent
                originalTranscription = state.transcription
                _uiState.update { it.copy(hasChanges = false) }
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "خطا در ذخیره: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            container.audioRecorderManager.stopPlayback()
            container.audioRecorderManager.deleteAudioFile(note.audioFilePath)
            container.noteRepository.deleteNote(note)
            onDeleted()
        }
    }

    fun playAudio(onComplete: () -> Unit = {}) {
        val path = _uiState.value.note?.audioFilePath ?: return
        container.audioRecorderManager.playAudio(path, onComplete)
    }

    fun stopAudio() {
        container.audioRecorderManager.stopPlayback()
    }

    class Factory(
        private val container: AppContainer,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteDetailModalViewModel(container, noteId) as T
        }
    }
}
