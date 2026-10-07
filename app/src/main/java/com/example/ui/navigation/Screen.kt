package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "MTE Merge")
    object Merge : Screen("merge", "Merge PDFs")
    object Split : Screen("split", "Split PDF")
    object ImageToPdf : Screen("image_to_pdf", "Image to PDF")
    object QrGenerator : Screen("qr_generator", "QR Generator")
    object LockPdf : Screen("lock_pdf", "Lock PDF (AES)")
    object UnlockPdf : Screen("unlock_pdf", "Unlock PDF")
    object History : Screen("history", "Recent History")
    object P2pShare : Screen("p2p_share", "Offline P2P Transfer")
    object NfcKeyEmulator : Screen("nfc_key_emulator", "Act as NFC Key")
    object PdfPreview : Screen("pdf_preview", "Preview Document")
}
