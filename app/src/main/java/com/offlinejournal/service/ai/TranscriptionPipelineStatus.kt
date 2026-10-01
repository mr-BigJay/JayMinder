package com.offlinejournal.service.ai

sealed class TranscriptionPipelineStatus {
    data object Idle : TranscriptionPipelineStatus()
    data object ConvertingSpeech : TranscriptionPipelineStatus()
    data object ImprovingText : TranscriptionPipelineStatus()
    data object Completed : TranscriptionPipelineStatus()
    data class SmartCleanupUnavailable(val message: String) : TranscriptionPipelineStatus()
}
