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
    val bitmap: Bitmap?,
    val extractedText: String,
    val keyTerms: List<String> = emptyList()
)

data class TextSelectionState(
    val selectedText: String = "",
    val pageIndex: Int = 0,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isVisible: Boolean = false
)
