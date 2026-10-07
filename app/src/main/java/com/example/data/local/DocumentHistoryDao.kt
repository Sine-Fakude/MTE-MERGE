package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentHistoryDao {
    @Query("SELECT * FROM document_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DocumentHistoryEntity>>

    @Query("SELECT * FROM document_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentHistory(): Flow<List<DocumentHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DocumentHistoryEntity): Long

    @Delete
    suspend fun delete(item: DocumentHistoryEntity)

    @Query("DELETE FROM document_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM document_history")
    suspend fun clearAll()
}
