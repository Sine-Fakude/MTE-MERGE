package com.example.domain.model

import android.net.Uri
import java.util.UUID

data class PdfItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 0
) {
    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1024 * 1024 -> String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0))
            fileSizeBytes >= 1024 -> String.format("%.1f KB", fileSizeBytes / 1024.0)
            fileSizeBytes > 0 -> "$fileSizeBytes B"
            else -> "Unknown size"
        }
}
