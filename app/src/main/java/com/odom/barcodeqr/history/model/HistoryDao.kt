package com.odom.barcodeqr.history.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(memo: HistoryItem)

    @Query("SELECT * FROM HistoryItem ORDER BY id DESC")
    fun getAll(): Flow<List<HistoryItem>>
}