package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MteMergeApp
import com.example.domain.model.OperationResult
import com.example.domain.model.PdfItem
import com.example.domain.model.SplitConfig
import com.example.domain.model.SplitType
import com.example.domain.util.PdfManager
import com.example.domain.util.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitUiState(
    val selectedPdf: PdfItem? = null,
    val splitConfig: SplitConfig = SplitConfig(),
    val pageThumbnails: Map<Int, Bitmap> = emptyMap(),
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val result: OperationResult? = null,
    val errorMessage: String? = null
)

class SplitViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    private val repository = MteMergeApp.instance.documentRepository

    fun selectPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Reading PDF info...") }
            val item = PdfManager.getPdfItem(context, uri)

            _uiState.update {
                it.copy(
                    selectedPdf = item,
                    splitConfig = it.splitConfig.copy(
                        outputBaseName = item.fileName.removeSuffix(".pdf") + "_split"
                    ),
                    pageThumbnails = emptyMap(),
                    isLoading = false,
                    statusMessage = ""
                )
            }

            // Load thumbnails for pages (up to 20 pages for responsive UI)
            val maxThumbs = minOf(item.pageCount, 20)
            val thumbs = mutableMapOf<Int, Bitmap>()
            for (pageIdx in 0 until maxThumbs) {
                val bmp = PdfManager.renderPageThumbnail(context, uri, pageIdx)
                if (bmp != null) {
                    thumbs[pageIdx + 1] = bmp
                    _uiState.update { current ->
                        current.copy(pageThumbnails = current.pageThumbnails + (pageIdx + 1 to bmp))
                    }
                }
            }
        }
    }

    fun setSplitType(type: SplitType) {
        _uiState.update { it.copy(splitConfig = it.splitConfig.copy(splitType = type)) }
    }

    fun setRangeExpression(expr: String) {
        _uiState.update { it.copy(splitConfig = it.splitConfig.copy(rangeExpression = expr)) }
    }

    fun togglePageSelection(pageNumber: Int) {
        _uiState.update { current ->
            val set = current.splitConfig.selectedPages.toMutableSet()
            if (set.contains(pageNumber)) {
                set.remove(pageNumber)
            } else {
                set.add(pageNumber)
            }
            current.copy(splitConfig = current.splitConfig.copy(selectedPages = set))
        }
    }

    fun selectAllPages() {
        val total = _uiState.value.selectedPdf?.pageCount ?: 0
        if (total > 0) {
            _uiState.update {
                it.copy(splitConfig = it.splitConfig.copy(selectedPages = (1..total).toSet()))
            }
        }
    }

    fun clearPageSelection() {
        _uiState.update {
            it.copy(splitConfig = it.splitConfig.copy(selectedPages = emptySet()))
        }
    }

    fun setOutputBaseName(name: String) {
        _uiState.update { it.copy(splitConfig = it.splitConfig.copy(outputBaseName = name)) }
    }

    fun split(context: Context) {
        val pdf = _uiState.value.selectedPdf
        if (pdf == null) {
            _uiState.update { it.copy(errorMessage = "Please select a PDF document first") }
            return
        }

        val config = _uiState.value.splitConfig

        if (config.splitType == SplitType.EXTRACT_SELECTED && config.selectedPages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please tap to select at least one page to extract") }
            return
        }

        if (config.splitType == SplitType.PAGE_RANGE && config.rangeExpression.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please specify a page range (e.g., 1-3, 5)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Splitting document...", errorMessage = null) }
            val result = PdfManager.splitPdf(
                context = context,
                sourceUri = pdf.uri,
                sourceFileName = pdf.fileName,
                config = config
            )

            result.onSuccess { files ->
                if (files.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No pages were generated") }
                    return@onSuccess
                }

                val primaryFile = files.first()
                val primaryUri = ShareManager.getFileUri(context, primaryFile)
                val allUris = files.map { ShareManager.getFileUri(context, it) }

                // Record primary in history
                repository.recordOperation(
                    fileName = primaryFile.name,
                    filePath = primaryFile.absolutePath,
                    uriString = primaryUri.toString(),
                    operationType = "SPLIT",
                    fileSizeBytes = files.sumOf { it.length() },
                    pageCount = files.size
                )

                val opResult = OperationResult(
                    title = "PDF Split Complete",
                    primaryUri = primaryUri,
                    filePath = primaryFile.absolutePath,
                    fileName = primaryFile.name,
                    fileSizeBytes = files.sumOf { it.length() },
                    mimeType = "application/pdf",
                    pageCount = files.size,
                    additionalUris = allUris,
                    message = if (files.size == 1) "Created 1 PDF file" else "Generated ${files.size} PDF files"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "",
                        result = opResult
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "",
                        errorMessage = err.localizedMessage ?: "Failed to split PDF"
                    )
                }
            }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(result = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        _uiState.update {
            SplitUiState()
        }
    }
}
