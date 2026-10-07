package com.example.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.PdfItem
import com.example.domain.model.SplitConfig
import com.example.domain.model.SplitType
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object PdfManager {

    suspend fun getPdfItem(context: Context, uri: Uri): PdfItem = withContext(Dispatchers.IO) {
        var displayName = "Document.pdf"
        var sizeBytes = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
            }
        }

        var pageCount = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                PDDocument.load(stream).use { doc ->
                    pageCount = doc.numberOfPages
                }
            }
        } catch (_: Exception) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                    }
                }
            } catch (_: Exception) {}
        }

        PdfItem(
            uri = uri,
            fileName = displayName,
            fileSizeBytes = sizeBytes,
            pageCount = pageCount
        )
    }

    suspend fun mergePdfs(
        context: Context,
        pdfItems: List<PdfItem>,
        outputFileName: String,
        password: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (pdfItems.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No PDF files selected to merge"))
            }

            val sanitizedName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputDir = File(context.cacheDir, "merged_pdfs").apply { mkdirs() }
            val outputFile = File(outputDir, "${System.currentTimeMillis()}_$sanitizedName")

            val merger = PDFMergerUtility()
            merger.destinationFileName = outputFile.absolutePath

            val tempFiles = mutableListOf<File>()
            try {
                for (item in pdfItems) {
                    val temp = File.createTempFile("merge_src_", ".pdf", context.cacheDir)
                    tempFiles.add(temp)
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    merger.addSource(temp)
                }

                // Route heavy scratch memory to temporary device storage to support large 20+ PDF batch merges
                val memorySettings = MemoryUsageSetting.setupTempFileOnly().setTempDir(context.cacheDir)
                merger.mergeDocuments(memorySettings)

                if (!password.isNullOrBlank()) {
                    encryptPdfFile(context, outputFile, password)
                }

                Result.success(outputFile)
            } finally {
                for (temp in tempFiles) {
                    temp.delete()
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun encryptPdfFile(context: Context, file: File, password: String, keyLength: Int = 128) {
        var doc: PDDocument? = null
        try {
            val memorySettings = MemoryUsageSetting.setupTempFileOnly().setTempDir(context.cacheDir)
            doc = PDDocument.load(file, memorySettings)
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = keyLength
                permissions = ap
            }
            doc.protect(spp)
            val tempEncrypted = File(file.parentFile, "enc_${System.currentTimeMillis()}_${file.name}")
            doc.save(tempEncrypted)
            tempEncrypted.copyTo(file, overwrite = true)
            tempEncrypted.delete()
        } finally {
            doc?.close()
        }
    }

    /**
     * Batch Lock PDF action.
     * Processes multiple PDFs in parallel using Kotlin Coroutines (async/awaitAll)
     * and strictly closes every PDDocument in a finally block to prevent memory leaks.
     */
    suspend fun batchLockPdfs(
        context: Context,
        pdfItems: List<PdfItem>,
        password: String,
        keyLength: Int = 128
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Password cannot be blank"))
        }
        if (pdfItems.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No PDF files selected to lock"))
        }

        try {
            val outputDir = File(context.cacheDir, "locked_pdfs").apply { mkdirs() }

            coroutineScope {
                val deferredList = pdfItems.map { item ->
                    async(Dispatchers.IO) {
                        val baseName = item.fileName.removeSuffix(".pdf")
                        val outputFile = File(outputDir, "${System.currentTimeMillis()}_${baseName}_locked.pdf")

                        var inputStream: InputStream? = null
                        var doc: PDDocument? = null
                        try {
                            inputStream = context.contentResolver.openInputStream(item.uri)
                                ?: throw IllegalStateException("Could not open ${item.fileName}")
                            val memorySettings = MemoryUsageSetting.setupTempFileOnly().setTempDir(context.cacheDir)
                            doc = PDDocument.load(inputStream, memorySettings)
                            val ap = AccessPermission()
                            val spp = StandardProtectionPolicy(password, password, ap).apply {
                                encryptionKeyLength = keyLength
                                permissions = ap
                            }
                            doc.protect(spp)
                            doc.save(outputFile)
                            outputFile
                        } finally {
                            doc?.close()
                            inputStream?.close()
                        }
                    }
                }
                val files = deferredList.awaitAll()
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch Unlock PDF action.
     * Processes multiple encrypted PDFs in parallel using async/awaitAll.
     */
    suspend fun batchUnlockPdfs(
        context: Context,
        pdfItems: List<PdfItem>,
        password: String
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        if (pdfItems.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No PDF files selected to unlock"))
        }

        try {
            val outputDir = File(context.cacheDir, "unlocked_pdfs").apply { mkdirs() }

            coroutineScope {
                val deferredList = pdfItems.map { item ->
                    async(Dispatchers.IO) {
                        val baseName = item.fileName.removeSuffix(".pdf")
                        val outputFile = File(outputDir, "${System.currentTimeMillis()}_${baseName}_unlocked.pdf")

                        var inputStream: InputStream? = null
                        var doc: PDDocument? = null
                        try {
                            inputStream = context.contentResolver.openInputStream(item.uri)
                                ?: throw IllegalStateException("Could not open ${item.fileName}")
                            val memorySettings = MemoryUsageSetting.setupTempFileOnly().setTempDir(context.cacheDir)
                            doc = PDDocument.load(inputStream, password, memorySettings)
                            doc.setAllSecurityToBeRemoved(true)
                            doc.save(outputFile)
                            outputFile
                        } catch (e: InvalidPasswordException) {
                            throw IllegalArgumentException("Incorrect password for ${item.fileName}")
                        } finally {
                            doc?.close()
                            inputStream?.close()
                        }
                    }
                }
                val files = deferredList.awaitAll()
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun splitPdf(
        context: Context,
        sourceUri: Uri,
        sourceFileName: String,
        config: SplitConfig
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val outputDir = File(context.cacheDir, "split_pdfs").apply { mkdirs() }
            val baseName = config.outputBaseName.ifBlank {
                sourceFileName.removeSuffix(".pdf")
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open source PDF"))

            val results = mutableListOf<File>()

            try {
                val memorySettings = MemoryUsageSetting.setupTempFileOnly().setTempDir(context.cacheDir)
                PDDocument.load(inputStream, memorySettings).use { sourceDoc ->
                    val totalPages = sourceDoc.numberOfPages
                    if (totalPages == 0) {
                        return@withContext Result.failure(IllegalStateException("Document has 0 pages"))
                    }

                    when (config.splitType) {
                        SplitType.ALL_PAGES -> {
                            val splitter = Splitter()
                            val splitDocs = splitter.split(sourceDoc)
                            splitDocs.forEachIndexed { index, splitDoc ->
                                try {
                                    val pageNum = index + 1
                                    val file = File(outputDir, "${baseName}_page_${pageNum}.pdf")
                                    splitDoc.save(file)
                                    results.add(file)
                                } finally {
                                    splitDoc.close()
                                }
                            }
                        }

                        SplitType.EXTRACT_SELECTED -> {
                            val selected = config.selectedPages.filter { it in 1..totalPages }.sorted()
                            if (selected.isEmpty()) {
                                return@withContext Result.failure(IllegalArgumentException("No valid pages selected"))
                            }

                            val newDoc = PDDocument()
                            try {
                                for (pageNumber in selected) {
                                    newDoc.addPage(sourceDoc.getPage(pageNumber - 1))
                                }
                                val file = File(outputDir, "${baseName}_extracted.pdf")
                                newDoc.save(file)
                                results.add(file)
                            } finally {
                                newDoc.close()
                            }
                        }

                        SplitType.PAGE_RANGE -> {
                            val ranges = parseRangeExpression(config.rangeExpression, totalPages)
                            if (ranges.isEmpty()) {
                                return@withContext Result.failure(IllegalArgumentException("Invalid page range specified"))
                            }

                            ranges.forEachIndexed { _, pageRange ->
                                val newDoc = PDDocument()
                                try {
                                    for (p in pageRange) {
                                        newDoc.addPage(sourceDoc.getPage(p - 1))
                                    }
                                    val suffix = if (pageRange.size == 1) "p${pageRange.first()}" else "p${pageRange.first()}-${pageRange.last()}"
                                    val file = File(outputDir, "${baseName}_range_${suffix}.pdf")
                                    newDoc.save(file)
                                    results.add(file)
                                } finally {
                                    newDoc.close()
                                }
                            }
                        }
                    }
                }
                Result.success(results)
            } finally {
                inputStream.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseRangeExpression(expression: String, maxPage: Int): List<List<Int>> {
        val parts = expression.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<List<Int>>()

        for (part in parts) {
            if (part.contains("-")) {
                val sub = part.split("-").map { it.trim() }
                if (sub.size == 2) {
                    val start = sub[0].toIntOrNull() ?: continue
                    val end = sub[1].toIntOrNull() ?: continue
                    val validStart = min(start, end).coerceIn(1, maxPage)
                    val validEnd = maxOf(start, end).coerceIn(1, maxPage)
                    if (validStart <= validEnd) {
                        result.add((validStart..validEnd).toList())
                    }
                }
            } else {
                val single = part.toIntOrNull() ?: continue
                if (single in 1..maxPage) {
                    result.add(listOf(single))
                }
            }
        }
        return result
    }

    suspend fun renderPageThumbnail(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 260
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val aspectRatio = page.height.toFloat() / page.width.toFloat()
                            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            return@withContext bitmap
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
