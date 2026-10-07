package com.example.domain.model

enum class SplitType(val title: String, val description: String) {
    ALL_PAGES("Split Every Page", "Extracts every page into individual single-page PDF files"),
    PAGE_RANGE("Split by Page Range", "Specify custom page ranges (e.g., 1-3, 5, 8-12)"),
    EXTRACT_SELECTED("Extract Selected Pages", "Choose specific pages and bundle them into a single PDF")
}

data class SplitConfig(
    val splitType: SplitType = SplitType.ALL_PAGES,
    val rangeExpression: String = "",
    val selectedPages: Set<Int> = emptySet(), // 1-indexed
    val outputBaseName: String = "split_document"
)
