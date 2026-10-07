package com.example.domain.model

enum class PageOrientation(val label: String) {
    AUTO("Auto Detect"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class PageSizeFormat(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (595 × 842 pt)", 595, 842),
    LETTER("Letter (612 × 792 pt)", 612, 792),
    FIT_TO_IMAGE("Fit Image Dimensions", 0, 0)
}

enum class PageMargin(val label: String, val marginPt: Int) {
    NONE("No Margin", 0),
    SMALL("Small (16 pt)", 16),
    MEDIUM("Medium (32 pt)", 32)
}

enum class CompressionLevel(
    val title: String,
    val description: String,
    val maxDimensionPx: Int,
    val jpegQuality: Int
) {
    FAST_WEB("Fast / Web", "Max 720p • 50% Quality", 720, 50),
    MEDIUM("Medium", "Max 1080p • 75% Quality", 1080, 75),
    HIGH_QUALITY("High Quality", "Original • 100% Quality", Int.MAX_VALUE, 100)
}

data class ImageToPdfConfig(
    val orientation: PageOrientation = PageOrientation.AUTO,
    val pageSize: PageSizeFormat = PageSizeFormat.A4,
    val margin: PageMargin = PageMargin.SMALL,
    val compression: CompressionLevel = CompressionLevel.MEDIUM,
    val outputFileName: String = "Images_Merged.pdf",
    val isEncrypted: Boolean = false,
    val password: String = ""
)
