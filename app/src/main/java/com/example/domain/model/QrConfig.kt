package com.example.domain.model

import android.graphics.Color
import android.net.Uri

enum class QrType(val label: String) {
    TEXT("Text"),
    URL("Website / Link"),
    WIFI("Wi-Fi Network"),
    EMAIL("Email"),
    PHONE("Phone"),
    SMS("SMS Message"),
    VCARD("Contact Card")
}

data class QrConfig(
    val content: String = "",
    val qrType: QrType = QrType.TEXT,
    val foregroundColor: Int = Color.parseColor("#0F172A"),
    val backgroundColor: Int = Color.WHITE,
    val errorCorrectionLevel: String = "H", // H gives 30% error correction, essential for logos
    val withCenterBadge: Boolean = true,
    val logoUri: Uri? = null,
    val sizePx: Int = 800
)

data class WifiQrData(
    val ssid: String = "",
    val password: String = "",
    val security: String = "WPA", // WPA, WEP, nopass
    val isHidden: Boolean = false
) {
    fun toQrString(): String {
        return "WIFI:T:$security;S:$ssid;P:$password;H:${if (isHidden) "true" else "false"};;"
    }
}

data class ContactQrData(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val org: String = ""
) {
    fun toVCard(): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (name.isNotBlank()) appendLine("FN:$name")
            if (org.isNotBlank()) appendLine("ORG:$org")
            if (phone.isNotBlank()) appendLine("TEL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL:$email")
            appendLine("END:VCARD")
        }
    }
}
