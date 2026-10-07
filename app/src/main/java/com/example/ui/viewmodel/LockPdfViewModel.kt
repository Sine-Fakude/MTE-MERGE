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

data class LockPdfUiState(
    val selectedPdfs: List<PdfItem> = emptyList(),
    val password: String = "",
    val isNfcKey: Boolean = false,
    val nfcHardwareUid: String? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val result: OperationResult? = null,
    val errorMessage: String? = null
)

class LockPdfViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LockPdfUiState())
    val uiState: StateFlow<LockPdfUiState> = _uiState.asStateFlow()

    private val repository = MteMergeApp.instance.documentRepository

    fun addPdfs(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Inspecting PDF files...") }
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

    fun addPdfFromUri(context: Context, uri: Uri) {
        addPdfs(context, listOf(uri))
    }

    fun removePdf(item: PdfItem) {
        _uiState.update { current ->
            current.copy(selectedPdfs = current.selectedPdfs.filter { it.id != item.id })
        }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, isNfcKey = false, nfcHardwareUid = null) }
    }

    fun setNfcKey(key: String, uid: String) {
        _uiState.update { it.copy(password = key, isNfcKey = true, nfcHardwareUid = uid, errorMessage = null) }
    }

    fun lockBatch(context: Context) {
        val pdfs = _uiState.value.selectedPdfs
        if (pdfs.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one PDF to lock") }
            return
        }
        val pwd = _uiState.value.password
        if (pwd.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an encryption password or scan an NFC key") }
            return
        }

        val isNfc = _uiState.value.isNfcKey
        val keyLength = if (isNfc) 256 else 128

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = "Locking ${pdfs.size} PDF document(s) in parallel with AES-$keyLength..."
                )
            }

            val result = PdfManager.batchLockPdfs(context, pdfs, pwd, keyLength = keyLength)

            result.onSuccess { files ->
                val uris = files.map { ShareManager.getFileUri(context, it) }
                val firstFile = files.first()
                val firstUri = uris.first()

                // Record operations into Room DB
                files.forEachIndexed { index, file ->
                    val orig = pdfs.getOrNull(index)
                    repository.recordOperation(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        uriString = ShareManager.getFileUri(context, file).toString(),
                        operationType = "LOCK",
                        fileSizeBytes = file.length(),
                        pageCount = orig?.pageCount ?: 1
                    )
                }

                val opResult = OperationResult(
                    title = if (files.size > 1) "Batch Locked (${files.size} PDFs)" else "PDF Locked Successfully",
                    primaryUri = firstUri,
                    filePath = firstFile.absolutePath,
                    fileName = firstFile.name,
                    fileSizeBytes = files.sumOf { it.length() },
                    mimeType = "application/pdf",
                    pageCount = pdfs.sumOf { it.pageCount },
                    additionalUris = if (uris.size > 1) uris else emptyList(),
                    message = "Successfully encrypted ${files.size} file(s) with standard AES security"
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
                        errorMessage = err.localizedMessage ?: "Failed to lock PDF documents"
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
                password = "",
                result = null,
                errorMessage = null,
                statusMessage = ""
            )
        }
    }
}
