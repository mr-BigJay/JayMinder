package com.offlinejournal.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val createdAtMillis: Long
)
