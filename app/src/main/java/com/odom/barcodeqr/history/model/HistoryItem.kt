package com.odom.barcodeqr.history.model

import androidx.room.PrimaryKey
import androidx.room.Entity


@Entity
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // val qrtime : String,
    val qrString : String
)
