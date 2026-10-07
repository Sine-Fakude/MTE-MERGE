package com.example.domain.model

import android.net.Uri

data class OperationResult(
    val title: String,
    val primaryUri: Uri,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "application/pdf",
    val pageCount: Int? = null,
    val additionalUris: List<Uri> = emptyList(),
    val message: String = ""
) {
    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1024 * 1024 -> String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0))
            fileSizeBytes >= 1024 -> String.format("%.1f KB", fileSizeBytes / 1024.0)
            fileSizeBytes > 0 -> "$fileSizeBytes B"
            else -> ""
        }
}
