package com.example.data.repository

import com.example.data.local.DocumentHistoryDao
import com.example.data.local.DocumentHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class DocumentRepository(
    private val dao: DocumentHistoryDao
) {
    val allHistory: Flow<List<DocumentHistoryEntity>> = dao.getAllHistory()
    val recentHistory: Flow<List<DocumentHistoryEntity>> = dao.getRecentHistory()

    suspend fun recordOperation(
        fileName: String,
        filePath: String,
        uriString: String,
        operationType: String,
        fileSizeBytes: Long,
        pageCount: Int
    ): Long {
        val entity = DocumentHistoryEntity(
            fileName = fileName,
            filePath = filePath,
            uriString = uriString,
            operationType = operationType,
            fileSizeBytes = fileSizeBytes,
            pageCount = pageCount
        )
        return dao.insert(entity)
    }

    suspend fun deleteHistory(item: DocumentHistoryEntity) {
        // Also delete underlying file if it exists in cache
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.delete(item)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
