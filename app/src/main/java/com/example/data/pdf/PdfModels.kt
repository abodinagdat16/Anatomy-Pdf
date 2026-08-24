package com.example.data.pdf

import android.graphics.Bitmap
import android.net.Uri

data class PdfDocumentItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val pageCount: Int,
    val uri: Uri? = null,
    val isPreset: Boolean = false,
    val topicTag: String = "Anatomy"
)

data class PdfPageData(
    val pageIndex: Int,
    val totalPages: Int = 1,
    val bitmap: Bitmap? = null,
    val text: String = "",
    val words: List<ExtractedPdfWord> = emptyList(),
    val keyTerms: List<String> = emptyList()
)

data class TextSelectionState(
    val selectedText: String = "",
    val pageIndex: Int = 0,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isVisible: Boolean = false
)

data class PdfActiveSelection(
    val text: String,
    val pageIndex: Int,
    val selectedWordIndices: Set<Int>,
    val menuAnchorX: Float = 0.5f,
    val menuAnchorY: Float = 0.5f
)
