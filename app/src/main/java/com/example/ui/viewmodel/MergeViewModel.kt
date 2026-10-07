package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MteMergeApp
import com.example.domain.model.OperationResult
import com.example.domain.model.PdfItem
import com.example.domain.util.PdfManager
import com.example.domain.util.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MergeUiState(
    val selectedPdfs: List<PdfItem> = emptyList(),
    val outputFileName: String = "Merged_Document",
    val isEncrypted: Boolean = false,
    val password: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val result: OperationResult? = null,
    val errorMessage: String? = null
)

class MergeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState.asStateFlow()

    private val repository = MteMergeApp.instance.documentRepository

    fun addPdfs(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Reading PDF info...") }
            val newItems = mutableListOf<PdfItem>()
            for (uri in uris) {
                val item = PdfManager.getPdfItem(context, uri)
                newItems.add(item)
            }

            _uiState.update { current ->
                current.copy(
                    selectedPdfs = current.selectedPdfs + newItems,
                    isLoading = false,
                    statusMessage = ""
                )
            }
        }
    }

    fun prefillWithUri(context: Context, uri: Uri) {
        _uiState.update { it.copy(selectedPdfs = emptyList(), result = null, errorMessage = null) }
        addPdfs(context, listOf(uri))
    }

    fun removePdf(item: PdfItem) {
        _uiState.update { current ->
            current.copy(selectedPdfs = current.selectedPdfs.filter { it.id != item.id })
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.selectedPdfs.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _uiState.update { it.copy(selectedPdfs = list) }
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        reorder(index, index - 1)
    }

    fun moveDown(index: Int) {
        val list = _uiState.value.selectedPdfs
        if (index >= list.size - 1) return
        reorder(index, index + 1)
    }

    fun updateOutputFileName(name: String) {
        _uiState.update { it.copy(outputFileName = name) }
    }

    fun setEncrypted(enabled: Boolean) {
        _uiState.update { it.copy(isEncrypted = enabled) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun merge(context: Context) {
        val items = _uiState.value.selectedPdfs
        if (items.size < 2) {
            _uiState.update { it.copy(errorMessage = "Please select at least 2 PDF files to merge") }
            return
        }

        if (_uiState.value.isEncrypted && _uiState.value.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an encryption password") }
            return
        }

        val outputName = _uiState.value.outputFileName.ifBlank { "Merged_Document" }
        val passwordToApply = if (_uiState.value.isEncrypted) _uiState.value.password else null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Merging ${items.size} documents...", errorMessage = null) }
            val mergeResult = PdfManager.mergePdfs(context, items, outputName, passwordToApply)

            mergeResult.onSuccess { outputFile ->
                val fileUri = ShareManager.getFileUri(context, outputFile)
                val totalPages = items.sumOf { it.pageCount }

                repository.recordOperation(
                    fileName = outputFile.name,
                    filePath = outputFile.absolutePath,
                    uriString = fileUri.toString(),
                    operationType = "MERGE",
                    fileSizeBytes = outputFile.length(),
                    pageCount = totalPages
                )

                val result = OperationResult(
                    title = if (_uiState.value.isEncrypted) "PDFs Merged & Encrypted (AES)" else "PDFs Merged Successfully",
                    primaryUri = fileUri,
                    filePath = outputFile.absolutePath,
                    fileName = outputFile.name,
                    fileSizeBytes = outputFile.length(),
                    mimeType = "application/pdf",
                    pageCount = totalPages,
                    message = "Merged ${items.size} files (${totalPages} total pages)" +
                            if (_uiState.value.isEncrypted) " • Password Protected" else ""
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "",
                        result = result
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "",
                        errorMessage = error.localizedMessage ?: "Failed to merge PDF files"
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

    fun clearAll() {
        _uiState.update {
            it.copy(
                selectedPdfs = emptyList(),
                result = null,
                errorMessage = null,
                statusMessage = "",
                isEncrypted = false,
                password = ""
            )
        }
    }
}
