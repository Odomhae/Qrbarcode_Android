package com.odom.barcodeqr.history.model

import androidx.room.PrimaryKey
import androidx.room.Entity


@Entity
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val qrString : String,
    val createdAt: Long = System.currentTimeMillis()
)
