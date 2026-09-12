package com.offlinejournal.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: String? = null
)

class BackupViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = container.backupManager.exportToDownloads()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = result.fold(
                        onSuccess = { name -> "پشتیبان در دانلودها ذخیره شد:\n$name" },
                        onFailure = { e -> "خطا در پشتیبان‌گیری: ${e.message}" }
                    )
                )
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = container.backupManager.importFromUri(uri)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = result.fold(
                        onSuccess = { "داده‌ها با موفقیت بازگردانی شدند." },
                        onFailure = { e -> "خطا در بازگردانی: ${e.message}" }
                    )
                )
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(container) as T
        }
    }
}
