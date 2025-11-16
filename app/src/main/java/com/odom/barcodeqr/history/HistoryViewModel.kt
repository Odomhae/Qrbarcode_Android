package com.odom.barcodeqr.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odom.barcodeqr.history.model.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HistoryDatabase.getInstance(application).historyDao()
    val listHistory = dao.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addHistory(content: String) {
        viewModelScope.launch {
            dao.insert(HistoryItem(qrString = content))
        }
    }
}
