package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MteMergeApp
import com.example.domain.model.CompressionLevel
import com.example.domain.model.ImageItem
import com.example.domain.model.ImageToPdfConfig
import com.example.domain.model.OperationResult
import com.example.domain.model.PageMargin
import com.example.domain.model.PageOrientation
import com.example.domain.model.PageSizeFormat
import com.example.domain.util.ImageToPdfConverter
import com.example.domain.util.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageToPdfUiState(
    val selectedImages: List<ImageItem> = emptyList(),
    val config: ImageToPdfConfig = ImageToPdfConfig(),
    val compressionSliderValue: Float = 1f, // 0 = Fast/Web, 1 = Medium, 2 = High Quality
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val result: OperationResult? = null,
    val errorMessage: String? = null
)

class ImageToPdfViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()

    private val repository = MteMergeApp.instance.documentRepository

    fun addImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Adding images...") }
            val newItems = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    var displayName = "Image_${System.currentTimeMillis()}"
                    var sizeBytes = 0L
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                            if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                    ImageItem(
                        uri = uri,
                        fileName = displayName,
                        fileSizeBytes = sizeBytes
                    )
                }
            }

            _uiState.update { current ->
                current.copy(
                    selectedImages = current.selectedImages + newItems,
                    isLoading = false,
                    statusMessage = ""
                )
            }
        }
    }

    fun removeImage(item: ImageItem) {
        _uiState.update { current ->
            current.copy(selectedImages = current.selectedImages.filter { it.id != item.id })
        }
    }

    fun rotateImage(item: ImageItem) {
        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages.map {
                    if (it.id == item.id) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360)
                    else it
                }
            )
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.selectedImages.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _uiState.update { it.copy(selectedImages = list) }
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        reorder(index, index - 1)
    }

    fun moveDown(index: Int) {
        val list = _uiState.value.selectedImages
        if (index >= list.size - 1) return
        reorder(index, index + 1)
    }

    fun setCompressionSlider(value: Float) {
        val level = when (value.toInt()) {
            0 -> CompressionLevel.FAST_WEB
            2 -> CompressionLevel.HIGH_QUALITY
            else -> CompressionLevel.MEDIUM
        }
        _uiState.update {
            it.copy(
                compressionSliderValue = value,
                config = it.config.copy(compression = level)
            )
        }
    }

    fun setOrientation(orientation: PageOrientation) {
        _uiState.update { it.copy(config = it.config.copy(orientation = orientation)) }
    }

    fun setPageSize(pageSize: PageSizeFormat) {
        _uiState.update { it.copy(config = it.config.copy(pageSize = pageSize)) }
    }

    fun setMargin(margin: PageMargin) {
        _uiState.update { it.copy(config = it.config.copy(margin = margin)) }
    }

    fun setOutputFileName(name: String) {
        _uiState.update { it.copy(config = it.config.copy(outputFileName = name)) }
    }

    fun setEncrypted(enabled: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(isEncrypted = enabled)) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(config = it.config.copy(password = password)) }
    }

    fun convert(context: Context) {
        val images = _uiState.value.selectedImages
        if (images.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least 1 image to convert") }
            return
        }

        val config = _uiState.value.config
        if (config.isEncrypted && config.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an encryption password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Optimizing and converting images...", errorMessage = null) }
            val result = ImageToPdfConverter.convertImagesToPdf(context, images, config)

            result.onSuccess { file ->
                val fileUri = ShareManager.getFileUri(context, file)

                repository.recordOperation(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    uriString = fileUri.toString(),
                    operationType = "IMAGE_TO_PDF",
                    fileSizeBytes = file.length(),
                    pageCount = images.size
                )

                val opResult = OperationResult(
                    title = if (config.isEncrypted) "PDF Created & Encrypted (AES)" else "PDF Created Successfully",
                    primaryUri = fileUri,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                    mimeType = "application/pdf",
                    pageCount = images.size,
                    message = "Converted ${images.size} photos into PDF with ${config.compression.title} compression" +
                            if (config.isEncrypted) " • Password Protected" else ""
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
                        errorMessage = err.localizedMessage ?: "Failed to convert images to PDF"
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
                selectedImages = emptyList(),
                result = null,
                errorMessage = null,
                statusMessage = ""
            )
        }
    }
}
