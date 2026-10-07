package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PdfPreviewUiState(
    val uri: Uri? = null,
    val fileName: String = "Document.pdf",
    val pageCount: Int = 0,
    val currentPageIndex: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class PdfPreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfPreviewUiState())
    val uiState: StateFlow<PdfPreviewUiState> = _uiState.asStateFlow()

    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    // In-memory LRU cache sized to 25% of available memory (or max 40MB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 4).coerceAtMost(40 * 1024)

    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // Trigger state to notify composables when new pages are cached
    val cacheVersion = MutableStateFlow(0)

    fun loadDocument(uri: Uri, name: String) {
        if (_uiState.value.uri == uri && pdfRenderer != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    uri = uri,
                    fileName = name,
                    isLoading = true,
                    errorMessage = null
                )
            }

            withContext(Dispatchers.IO) {
                try {
                    closeCurrentRenderer()
                    bitmapCache.evictAll()

                    val context = getApplication<Application>()
                    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw IllegalStateException("Could not open file descriptor for URI")
                    pfd = descriptor

                    val renderer = PdfRenderer(descriptor)
                    pdfRenderer = renderer
                    val totalPages = renderer.pageCount

                    _uiState.update {
                        it.copy(
                            pageCount = totalPages,
                            isLoading = false
                        )
                    }

                    // Pre-render the first page
                    if (totalPages > 0) {
                        renderPage(0)
                    }
                } catch (e: Exception) {
                    Log.e("PdfPreviewViewModel", "Failed to load PDF", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to render PDF: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun getCachedPageBitmap(pageIndex: Int): Bitmap? {
        val cached = bitmapCache.get(pageIndex)
        if (cached == null && pdfRenderer != null && pageIndex in 0 until (_uiState.value.pageCount)) {
            // Request background rendering on Dispatchers.IO
            viewModelScope.launch(Dispatchers.IO) {
                renderPage(pageIndex)
            }
        }
        return cached
    }

    private suspend fun renderPage(pageIndex: Int) = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext
        if (pageIndex !in 0 until renderer.pageCount) return@withContext
        if (bitmapCache.get(pageIndex) != null) return@withContext

        try {
            // Open page, render to bitmap, and immediately close page
            val page = renderer.openPage(pageIndex)
            try {
                // High fidelity rendering: scale to 2x or 1080p target width
                val targetWidth = 1200
                val scale = targetWidth.toFloat() / page.width.toFloat()
                val width = targetWidth
                val height = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmapCache.put(pageIndex, bitmap)
                cacheVersion.update { it + 1 }
            } finally {
                page.close()
            }
        } catch (e: Exception) {
            Log.e("PdfPreviewViewModel", "Error rendering page $pageIndex", e)
        }
    }

    fun onPageChanged(newIndex: Int) {
        _uiState.update { it.copy(currentPageIndex = newIndex) }
        // Pre-fetch next and previous page
        viewModelScope.launch(Dispatchers.IO) {
            renderPage(newIndex - 1)
            renderPage(newIndex + 1)
        }
    }

    private fun closeCurrentRenderer() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null

        try {
            pfd?.close()
        } catch (_: Exception) {}
        pfd = null
    }

    override fun onCleared() {
        super.onCleared()
        closeCurrentRenderer()
        bitmapCache.evictAll()
    }
}
