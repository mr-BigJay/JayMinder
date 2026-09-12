package com.offlinejournal.data.repository

import com.offlinejournal.data.local.dao.NoteDao
import com.offlinejournal.data.mapper.toDomain
import com.offlinejournal.data.mapper.toEntity
import com.offlinejournal.domain.model.DateFilterPeriod
import com.offlinejournal.domain.model.Note
import com.offlinejournal.util.DateRangeHelper
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val noteDao: NoteDao) {
    fun observeAllNotes(): Flow<List<Note>> =
        noteDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeById(id).map { it?.toDomain() }

    fun observeNotesByPeriod(
        period: DateFilterPeriod,
        categoryId: Long? = null,
        referenceMillis: Long = TehranTime.nowMillis()
    ): Flow<List<Note>> {
        val (start, end) = DateRangeHelper.rangeForPeriod(period, referenceMillis)
        return if (categoryId == null && period == DateFilterPeriod.ALL) {
            noteDao.observeAll().map { list -> list.map { it.toDomain() } }
        } else {
            noteDao.observeFiltered(categoryId, start, end)
                .map { list -> list.map { it.toDomain() } }
        }
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return observeAllNotes()
        return noteDao.search(trimmed).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getNote(id: Long): Note? = noteDao.getById(id)?.toDomain()

    suspend fun createNote(
        textContent: String,
        title: String? = null,
        audioFilePath: String? = null,
        transcription: String? = null,
        categoryId: Long? = null
    ): Long {
        val now = TehranTime.nowMillis()
        return noteDao.insert(
            Note(
                title = title?.trim()?.takeIf { it.isNotEmpty() },
                textContent = textContent,
                audioFilePath = audioFilePath,
                transcription = transcription,
                categoryId = categoryId,
                createdAtMillis = now,
                updatedAtMillis = now
            ).toEntity()
        )
    }

    suspend fun updateNote(note: Note) {
        noteDao.update(note.copy(updatedAtMillis = TehranTime.nowMillis()).toEntity())
    }

    suspend fun deleteNote(note: Note) {
        noteDao.delete(note.toEntity())
    }
}
