package com.odom.barcodeqr.history.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(memo: HistoryItem)

    @Query("SELECT * FROM HistoryItem ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HistoryItem>>

    @Delete
    suspend fun delete(historyItem: HistoryItem)

    @Query("DELETE FROM HistoryItem")
    suspend fun deleteAll()
}