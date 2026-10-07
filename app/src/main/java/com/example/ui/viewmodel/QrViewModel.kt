package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MteMergeApp
import com.example.domain.model.ContactQrData
import com.example.domain.model.OperationResult
import com.example.domain.model.QrConfig
import com.example.domain.model.QrType
import com.example.domain.model.WifiQrData
import com.example.domain.util.QrCodeGenerator
import com.example.domain.util.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QrUiState(
    val config: QrConfig = QrConfig(
        content = "https://",
        qrType = QrType.URL,
        foregroundColor = Color.parseColor("#0F172A"),
        backgroundColor = Color.WHITE,
        errorCorrectionLevel = "H",
        withCenterBadge = true,
        logoUri = null,
        sizePx = 800
    ),
    val rawTextInput: String = "https://",
    val wifiData: WifiQrData = WifiQrData(),
    val contactData: ContactQrData = ContactQrData(),
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val phoneNumber: String = "",
    val smsNumber: String = "",
    val smsBody: String = "",
    val generatedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val result: OperationResult? = null,
    val errorMessage: String? = null
)

class QrViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    private val repository = MteMergeApp.instance.documentRepository

    init {
        generateQr()
    }

    fun setQrType(type: QrType) {
        _uiState.update { current ->
            val defaultContent = when (type) {
                QrType.TEXT -> "Welcome to MTE Merge"
                QrType.URL -> "https://"
                QrType.WIFI -> current.wifiData.toQrString()
                QrType.VCARD -> current.contactData.toVCard()
                QrType.EMAIL -> "mailto:"
                QrType.PHONE -> "tel:"
                QrType.SMS -> "smsto:"
            }
            current.copy(
                config = current.config.copy(qrType = type, content = defaultContent),
                rawTextInput = if (type == QrType.TEXT || type == QrType.URL) defaultContent else current.rawTextInput
            )
        }
        generateQr()
    }

    fun updateRawText(text: String) {
        _uiState.update { current ->
            current.copy(
                rawTextInput = text,
                config = current.config.copy(content = text)
            )
        }
        generateQr()
    }

    fun updateWifi(wifi: WifiQrData) {
        _uiState.update { current ->
            current.copy(
                wifiData = wifi,
                config = current.config.copy(content = wifi.toQrString())
            )
        }
        generateQr()
    }

    fun updateContact(contact: ContactQrData) {
        _uiState.update { current ->
            current.copy(
                contactData = contact,
                config = current.config.copy(content = contact.toVCard())
            )
        }
        generateQr()
    }

    fun updateEmail(to: String, subject: String, body: String) {
        val mailto = "mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        _uiState.update { current ->
            current.copy(
                emailTo = to,
                emailSubject = subject,
                emailBody = body,
                config = current.config.copy(content = mailto)
            )
        }
        generateQr()
    }

    fun updatePhone(number: String) {
        _uiState.update { current ->
            current.copy(
                phoneNumber = number,
                config = current.config.copy(content = "tel:$number")
            )
        }
        generateQr()
    }

    fun updateSms(number: String, body: String) {
        _uiState.update { current ->
            current.copy(
                smsNumber = number,
                smsBody = body,
                config = current.config.copy(content = "smsto:$number:$body")
            )
        }
        generateQr()
    }

    fun setColors(foreground: Int, background: Int) {
        _uiState.update { current ->
            current.copy(
                config = current.config.copy(foregroundColor = foreground, backgroundColor = background)
            )
        }
        generateQr()
    }

    fun setLogoUri(uri: Uri?) {
        _uiState.update { current ->
            current.copy(
                config = current.config.copy(logoUri = uri, errorCorrectionLevel = "H")
            )
        }
        generateQr()
    }

    fun setErrorCorrection(level: String) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(errorCorrectionLevel = level))
        }
        generateQr()
    }

    fun toggleCenterBadge(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(withCenterBadge = enabled))
        }
        generateQr()
    }

    fun generateQr() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val bitmap = QrCodeGenerator.generateQrBitmap(app, _uiState.value.config)
                _uiState.update { it.copy(generatedBitmap = bitmap, isGenerating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun shareQrCode(context: Context) {
        val bmp = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch {
            try {
                val file = QrCodeGenerator.saveQrToFile(context, bmp, "MTE_QRCode.png")
                val uri = ShareManager.getFileUri(context, file)

                repository.recordOperation(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    uriString = uri.toString(),
                    operationType = "QR_CODE",
                    fileSizeBytes = file.length(),
                    pageCount = 1
                )

                // Trigger Android ACTION_SEND sheet immediately
                ShareManager.shareFile(context, file, mimeType = "image/png", chooserTitle = "Share QR Code")
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing QR code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveQrToDevice(context: Context) {
        val bmp = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch {
            try {
                val file = QrCodeGenerator.saveQrToFile(context, bmp, "MTE_QRCode_${System.currentTimeMillis()}.png")
                val saved = ShareManager.saveToDownloads(context, file, file.name, "image/png")
                if (saved) {
                    Toast.makeText(context, "QR Code saved to Downloads/MTE_Merge", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Saved to cache", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
