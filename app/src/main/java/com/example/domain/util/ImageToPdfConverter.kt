package com.example.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.domain.model.ImageItem
import com.example.domain.model.ImageToPdfConfig
import com.example.domain.model.PageMargin
import com.example.domain.model.PageOrientation
import com.example.domain.model.PageSizeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageToPdfConverter {

    suspend fun convertImagesToPdf(
        context: Context,
        images: List<ImageItem>,
        config: ImageToPdfConfig
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (images.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No images selected for conversion"))
            }

            val sanitizedName = if (config.outputFileName.endsWith(".pdf", ignoreCase = true)) {
                config.outputFileName
            } else {
                "${config.outputFileName}.pdf"
            }

            val outputDir = File(context.cacheDir, "images_pdf").apply { mkdirs() }
            val outputFile = File(outputDir, "${System.currentTimeMillis()}_$sanitizedName")

            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            images.forEachIndexed { index, imageItem ->
                val bitmap = loadOptimizedBitmap(context, imageItem, config)
                    ?: return@forEachIndexed

                // Determine target page width and height
                val (pageWidth, pageHeight) = calculatePageDimensions(
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    config = config
                )

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill background white
                canvas.drawColor(android.graphics.Color.WHITE)

                val margin = config.margin.marginPt.toFloat()
                val printableWidth = max(1f, pageWidth - (margin * 2))
                val printableHeight = max(1f, pageHeight - (margin * 2))

                // Scale bitmap to fit within printable area preserving aspect ratio
                val scale = minOf(
                    printableWidth / bitmap.width.toFloat(),
                    printableHeight / bitmap.height.toFloat()
                )

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val left = margin + (printableWidth - scaledWidth) / 2f
                val top = margin + (printableHeight - scaledHeight) / 2f

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)

                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            FileOutputStream(outputFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }
            pdfDocument.close()

            // Apply AES encryption if enabled
            if (config.isEncrypted && config.password.isNotBlank()) {
                PdfManager.encryptPdfFile(context, outputFile, config.password)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads and downsamples the image according to CompressionLevel (720p, 1080p, or Original)
     * and JPEG compression quality (50%, 75%, 100%).
     */
    private fun loadOptimizedBitmap(
        context: Context,
        imageItem: ImageItem,
        config: ImageToPdfConfig
    ): Bitmap? {
        val stream = context.contentResolver.openInputStream(imageItem.uri) ?: return null

        val maxDim = config.compression.maxDimensionPx
        val quality = config.compression.jpegQuality

        // First decode bounds
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(stream, null, options)
        stream.close()

        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        // Calculate inSampleSize
        var inSampleSize = 1
        if (maxDim < Int.MAX_VALUE) {
            while ((rawWidth / inSampleSize) > (maxDim * 1.5) || (rawHeight / inSampleSize) > (maxDim * 1.5)) {
                inSampleSize *= 2
            }
        }

        // Re-open stream to decode downsampled bitmap
        val decodeStream = context.contentResolver.openInputStream(imageItem.uri) ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        var baseBitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
        decodeStream.close()

        if (baseBitmap == null) return null

        // Apply rotation if needed
        if (imageItem.rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply {
                postRotate(imageItem.rotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
            if (rotated != baseBitmap) {
                baseBitmap.recycle()
                baseBitmap = rotated
            }
        }

        // Scale down to exact max bound if needed
        val currentMax = max(baseBitmap.width, baseBitmap.height)
        if (maxDim < Int.MAX_VALUE && currentMax > maxDim) {
            val scaleFactor = maxDim.toFloat() / currentMax.toFloat()
            val targetW = (baseBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
            val targetH = (baseBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(baseBitmap, targetW, targetH, true)
            if (scaled != baseBitmap) {
                baseBitmap.recycle()
                baseBitmap = scaled
            }
        }

        // Apply JPEG compression quality if < 100
        if (quality < 100) {
            val byteOut = ByteArrayOutputStream()
            baseBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOut)
            val compressedBytes = byteOut.toByteArray()
            val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
            if (compressedBmp != null) {
                baseBitmap.recycle()
                baseBitmap = compressedBmp
            }
        }

        return baseBitmap
    }

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        config: ImageToPdfConfig
    ): Pair<Int, Int> {
        val isBitmapLandscape = bitmapWidth > bitmapHeight

        if (config.pageSize == PageSizeFormat.FIT_TO_IMAGE) {
            val margin = config.margin.marginPt * 2
            return Pair(bitmapWidth + margin, bitmapHeight + margin)
        }

        var baseWidth = config.pageSize.widthPt
        var baseHeight = config.pageSize.heightPt

        when (config.orientation) {
            PageOrientation.AUTO -> {
                if (isBitmapLandscape && baseWidth < baseHeight) {
                    val tmp = baseWidth
                    baseWidth = baseHeight
                    baseHeight = tmp
                } else if (!isBitmapLandscape && baseWidth > baseHeight) {
                    val tmp = baseWidth
                    baseWidth = baseHeight
                    baseHeight = tmp
                }
            }
            PageOrientation.PORTRAIT -> {
                if (baseWidth > baseHeight) {
                    val tmp = baseWidth
                    baseWidth = baseHeight
                    baseHeight = tmp
                }
            }
            PageOrientation.LANDSCAPE -> {
                if (baseWidth < baseHeight) {
                    val tmp = baseWidth
                    baseWidth = baseHeight
                    baseHeight = tmp
                }
            }
        }

        return Pair(baseWidth, baseHeight)
    }
}
