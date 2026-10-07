package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MteMergeApp
import com.example.data.local.DocumentHistoryEntity
import com.example.domain.util.ShareManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel : ViewModel() {

    private val repository = MteMergeApp.instance.documentRepository

    val historyList: StateFlow<List<DocumentHistoryEntity>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentList: StateFlow<List<DocumentHistoryEntity>> = repository.recentHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteItem(item: DocumentHistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun openItem(context: Context, item: DocumentHistoryEntity) {
        val file = File(item.filePath)
        if (file.exists()) {
            val mimeType = if (item.operationType == "QR_CODE") "image/png" else "application/pdf"
            ShareManager.openFile(context, file, mimeType)
        }
    }

    fun shareItem(context: Context, item: DocumentHistoryEntity) {
        val file = File(item.filePath)
        if (file.exists()) {
            val mimeType = if (item.operationType == "QR_CODE") "image/png" else "application/pdf"
            ShareManager.shareFile(context, file, mimeType)
        }
    }
}
