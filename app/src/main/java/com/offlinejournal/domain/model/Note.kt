package com.offlinejournal.domain.model

data class Note(
    val id: Long = 0,
    val title: String? = null,
    val textContent: String,
    val audioFilePath: String? = null,
    val transcription: String? = null,
    val categoryId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    val displayText: String
        get() = when {
            !transcription.isNullOrBlank() -> transcription
            textContent.isNotBlank() -> textContent
            else -> ""
        }
}
