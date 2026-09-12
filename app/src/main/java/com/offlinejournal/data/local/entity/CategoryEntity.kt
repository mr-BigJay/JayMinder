package com.offlinejournal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Int = 0xFF4CAF50.toInt(),
    val createdAtMillis: Long
)
