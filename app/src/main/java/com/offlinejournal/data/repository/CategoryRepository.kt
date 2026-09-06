package com.offlinejournal.data.repository

import com.offlinejournal.data.local.dao.CategoryDao
import com.offlinejournal.data.mapper.toDomain
import com.offlinejournal.data.mapper.toEntity
import com.offlinejournal.domain.model.Category
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getCategory(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    suspend fun createCategory(name: String, colorArgb: Int = 0xFF4CAF50.toInt()): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("نام دسته‌بندی نمی‌تواند خالی باشد"))
        if (categoryDao.countByName(trimmed) > 0) {
            return Result.failure(IllegalArgumentException("این دسته‌بندی قبلاً وجود دارد"))
        }
        val id = categoryDao.insert(
            Category(
                name = trimmed,
                colorArgb = colorArgb,
                createdAtMillis = TehranTime.nowMillis()
            ).toEntity()
        )
        return Result.success(id)
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        val trimmed = category.name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("نام دسته‌بندی نمی‌تواند خالی باشد"))
        if (categoryDao.countByName(trimmed, category.id) > 0) {
            return Result.failure(IllegalArgumentException("این دسته‌بندی قبلاً وجود دارد"))
        }
        categoryDao.update(category.copy(name = trimmed).toEntity())
        return Result.success(Unit)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }
}
