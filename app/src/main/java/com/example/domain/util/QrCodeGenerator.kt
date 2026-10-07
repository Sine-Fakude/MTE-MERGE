package com.example.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.domain.model.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a styled QR code Bitmap with custom colors and center logo branding.
     */
    suspend fun generateQrBitmap(context: Context, config: QrConfig): Bitmap = withContext(Dispatchers.Default) {
        val content = config.content.ifBlank { "https://example.com" }
        val size = config.sizePx.coerceIn(200, 2048)

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
            // If custom logo or badge is requested, force H (30% error correction)
            val ecLevel = if (config.logoUri != null || config.withCenterBadge) {
                ErrorCorrectionLevel.H
            } else {
                when (config.errorCorrectionLevel) {
                    "L" -> ErrorCorrectionLevel.L
                    "M" -> ErrorCorrectionLevel.M
                    "Q" -> ErrorCorrectionLevel.Q
                    else -> ErrorCorrectionLevel.H
                }
            }
            put(EncodeHintType.ERROR_CORRECTION, ecLevel)
        }

        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height

        val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(config.backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.foregroundColor
            style = Paint.Style.FILL
        }

        // Draw QR modules
        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                if (bitMatrix.get(x, y)) {
                    canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                }
            }
        }

        // Handle uploaded custom logo OR center "MTE" emblem
        var customLogoBitmap: Bitmap? = null
        if (config.logoUri != null) {
            try {
                context.contentResolver.openInputStream(config.logoUri)?.use { stream ->
                    customLogoBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }

        if (customLogoBitmap != null) {
            // Crucial: Logo size restricted to max 25% of QR dimension (occupying ~6.25% of surface area, well within Reed-Solomon 30% budget)
            val logoSize = (size * 0.24f)
            val badgePadding = logoSize * 0.12f
            val badgeSize = logoSize + (badgePadding * 2f)

            val badgeLeft = (size - badgeSize) / 2f
            val badgeTop = (size - badgeSize) / 2f
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeSize, badgeTop + badgeSize)

            // Draw protective badge background
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.foregroundColor
                style = Paint.Style.STROKE
                strokeWidth = (size * 0.012f).coerceAtLeast(2f)
            }
            val badgeRadius = badgeSize * 0.24f
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBorderPaint)

            // Draw rounded custom logo
            val logoLeft = badgeLeft + badgePadding
            val logoTop = badgeTop + badgePadding
            val logoDestRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)

            val roundedLogo = getRoundedCornerBitmap(customLogoBitmap!!, logoSize.toInt(), badgeRadius * 0.7f)
            canvas.drawBitmap(roundedLogo, null, logoDestRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else if (config.withCenterBadge) {
            // Default MTE Emblem badge
            val badgeSize = (size * 0.20f)
            val badgeLeft = (size - badgeSize) / 2f
            val badgeTop = (size - badgeSize) / 2f
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeSize, badgeTop + badgeSize)

            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.foregroundColor
                style = Paint.Style.STROKE
                strokeWidth = (size * 0.012f).coerceAtLeast(2f)
            }
            val radius = badgeSize * 0.25f
            canvas.drawRoundRect(badgeRect, radius, radius, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, radius, radius, badgeBorderPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.foregroundColor
                textSize = badgeSize * 0.38f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val textY = badgeTop + (badgeSize / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText("MTE", size / 2f, textY, textPaint)
        }

        bitmap
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, targetSize: Int, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, targetSize, targetSize)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
        return output
    }

    suspend fun saveQrToFile(context: Context, bitmap: Bitmap, fileName: String = "QRCode.png"): File =
        withContext(Dispatchers.IO) {
            val sanitized = if (fileName.endsWith(".png", ignoreCase = true)) fileName else "$fileName.png"
            val outputDir = File(context.cacheDir, "generated_qr").apply { mkdirs() }
            val outputFile = File(outputDir, "${System.currentTimeMillis()}_$sanitized")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outputFile
        }
}
