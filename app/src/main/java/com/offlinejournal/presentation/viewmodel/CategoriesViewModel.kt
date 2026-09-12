package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val newCategoryName: String = "",
    val editingCategory: Category? = null,
    val errorMessage: String? = null
)

class CategoriesViewModel(private val container: AppContainer) : ViewModel() {
    val categories: StateFlow<List<Category>> = container.categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun updateNewCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name, errorMessage = null) }
    }

    fun startEditing(category: Category) {
        _uiState.update { it.copy(editingCategory = category, newCategoryName = category.name) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingCategory = null, newCategoryName = "", errorMessage = null) }
    }

    fun saveCategory() {
        val name = _uiState.value.newCategoryName
        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            val result = if (editing != null) {
                container.categoryRepository.updateCategory(editing.copy(name = name))
            } else {
                container.categoryRepository.createCategory(name)
            }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(editingCategory = null, newCategoryName = "", errorMessage = null)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            container.categoryRepository.deleteCategory(category)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(container) as T
        }
    }
}

class NoteDetailViewModel(
    private val container: AppContainer,
    noteId: Long
) : ViewModel() {
    val note: StateFlow<Note?> = container.noteRepository.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<Category>> = container.categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playAudio(filePath: String) {
        container.audioRecorderManager.playAudio(filePath)
    }

    fun deleteNote(note: Note, onDeleted: () -> Unit) {
        viewModelScope.launch {
            container.audioRecorderManager.deleteAudioFile(note.audioFilePath)
            container.noteRepository.deleteNote(note)
            onDeleted()
        }
    }

    class Factory(
        private val container: AppContainer,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteDetailViewModel(container, noteId) as T
        }
    }
}
