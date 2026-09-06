package com.offlinejournal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinejournal.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query(
        """
        SELECT * FROM notes
        WHERE createdAtMillis >= :startMillis AND createdAtMillis < :endMillis
        ORDER BY createdAtMillis DESC
        """
    )
    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
        AND createdAtMillis >= :startMillis AND createdAtMillis < :endMillis
        ORDER BY createdAtMillis DESC
        """
    )
    fun observeFiltered(
        categoryId: Long?,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
        OR textContent LIKE '%' || :query || '%'
        OR transcription LIKE '%' || :query || '%'
        ORDER BY createdAtMillis DESC
        """
    )
    fun search(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}
