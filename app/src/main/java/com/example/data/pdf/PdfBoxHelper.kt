package com.example.data.pdf

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class ExtractedPdfWord(
    val text: String,
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val pageIndex: Int
)

data class ExtractedPageResult(
    val pageIndex: Int,
    val fullText: String,
    val words: List<ExtractedPdfWord>
)

object PdfBoxHelper {
    private const val TAG = "PdfBoxHelper"
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
                Log.d(TAG, "PDFBox initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PDFBox", e)
            }
        }
    }

    /**
     * Extracts text and positional words for every page in a PDF file
     */
    suspend fun extractAllPages(
        context: Context,
        file: File
    ): List<ExtractedPageResult> = withContext(Dispatchers.IO) {
        init(context)
        val results = mutableListOf<ExtractedPageResult>()
        var document: PDDocument? = null

        try {
            document = PDDocument.load(file)
            val numPages = document.numberOfPages

            for (pageIdx in 0 until numPages) {
                val stripper = WordPositionStripper(pageIdx)
                val pdPage = document.getPage(pageIdx)
                val cropBox = pdPage.cropBox ?: pdPage.mediaBox
                val pageWidth = cropBox.width
                val pageHeight = cropBox.height
                val minBoxX = cropBox.lowerLeftX
                val minBoxY = cropBox.lowerLeftY

                stripper.setPageDimensions(pageWidth, pageHeight, minBoxX, minBoxY)
                stripper.startPage = pageIdx + 1
                stripper.endPage = pageIdx + 1
                
                val text = stripper.getText(document)
                var words = stripper.extractedWords

                if (words.isEmpty() && text.isNotBlank()) {
                    words = createFallbackWordBoxes(text, pageIdx).toMutableList()
                }

                results.add(
                    ExtractedPageResult(
                        pageIndex = pageIdx,
                        fullText = text.trim(),
                        words = words
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text with PDFBox from file", e)
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing document", e)
            }
        }

        return@withContext results
    }

    /**
     * Extracts text and positional words from an InputStream (e.g. content:// Uri)
     */
    suspend fun extractAllPagesFromStream(
        context: Context,
        inputStream: InputStream
    ): List<ExtractedPageResult> = withContext(Dispatchers.IO) {
        init(context)
        val results = mutableListOf<ExtractedPageResult>()
        var document: PDDocument? = null

        try {
            document = PDDocument.load(inputStream)
            val numPages = document.numberOfPages

            for (pageIdx in 0 until numPages) {
                val stripper = WordPositionStripper(pageIdx)
                val pdPage = document.getPage(pageIdx)
                val cropBox = pdPage.cropBox ?: pdPage.mediaBox
                val pageWidth = cropBox.width
                val pageHeight = cropBox.height
                val minBoxX = cropBox.lowerLeftX
                val minBoxY = cropBox.lowerLeftY

                stripper.setPageDimensions(pageWidth, pageHeight, minBoxX, minBoxY)
                stripper.startPage = pageIdx + 1
                stripper.endPage = pageIdx + 1

                val text = stripper.getText(document)
                var words = stripper.extractedWords

                if (words.isEmpty() && text.isNotBlank()) {
                    words = createFallbackWordBoxes(text, pageIdx).toMutableList()
                }

                results.add(
                    ExtractedPageResult(
                        pageIndex = pageIdx,
                        fullText = text.trim(),
                        words = words
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text with PDFBox from stream", e)
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing document", e)
            }
        }

        return@withContext results
    }

    /**
     * Helper to create normalized fallback word boxes from raw text lines
     * when a page has no extractable font stream.
     */
    fun createFallbackWordBoxes(text: String, pageIndex: Int): List<ExtractedPdfWord> {
        val result = mutableListOf<ExtractedPdfWord>()
        val lines = text.split("\n")
        val totalLines = maxOf(lines.size, 1)
        val lineSpacing = 0.85f / (totalLines + 4)
        val startY = 0.12f

        lines.forEachIndexed { lineIdx, line ->
            if (line.isNotBlank()) {
                val words = line.trim().split(Regex("\\s+"))
                val wordCount = maxOf(words.size, 1)
                val startX = 0.08f
                val availableWidth = 0.84f
                val wordWidth = (availableWidth / wordCount).coerceAtMost(0.25f)
                val y = startY + (lineIdx * lineSpacing)

                words.forEachIndexed { wordIdx, w ->
                    val clean = w.trim()
                    if (clean.isNotEmpty()) {
                        val x = startX + (wordIdx * (availableWidth / wordCount))
                        result.add(
                            ExtractedPdfWord(
                                text = clean,
                                normLeft = x,
                                normTop = y,
                                normRight = (x + wordWidth * 0.9f).coerceAtMost(0.96f),
                                normBottom = y + lineSpacing * 0.85f,
                                pageIndex = pageIndex
                            )
                        )
                    }
                }
            }
        }
        return result
    }
}

/**
 * Custom PDFTextStripper that tracks coordinates of each word on the page
 */
private class WordPositionStripper(private val targetPageIndex: Int) : PDFTextStripper() {
    val extractedWords = mutableListOf<ExtractedPdfWord>()
    private var pageWidth: Float = 595f
    private var pageHeight: Float = 842f
    private var cropBoxX: Float = 0f
    private var cropBoxY: Float = 0f

    private val currentWord = StringBuilder()
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE
    private var hasActiveWord = false

    init {
        sortByPosition = true
    }

    fun setPageDimensions(width: Float, height: Float, minX: Float = 0f, minY: Float = 0f) {
        this.pageWidth = if (width > 0) width else 595f
        this.pageHeight = if (height > 0) height else 842f
        this.cropBoxX = minX
        this.cropBoxY = minY
    }

    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (text == null || textPositions == null) return

        for (tp in textPositions) {
            val unicode = tp.unicode ?: ""
            if (unicode.isBlank() || unicode == " " || unicode == "\t" || unicode == "\n") {
                flushWord()
            } else {
                currentWord.append(unicode)
                val x = tp.xDirAdj
                val y = tp.yDirAdj - tp.heightDir // PDFBox yDirAdj is baseline
                val w = tp.widthDirAdj
                val h = tp.heightDir

                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x + w)
                maxY = maxOf(maxY, y + h)
                hasActiveWord = true
            }
        }
        flushWord()
        super.writeString(text, textPositions)
    }

    private fun flushWord() {
        if (hasActiveWord && currentWord.isNotEmpty()) {
            val wordText = currentWord.toString().trim()
            if (wordText.isNotEmpty()) {
                val normLeft = ((minX - cropBoxX) / pageWidth).coerceIn(0f, 1f)
                val normTop = ((minY - cropBoxY) / pageHeight).coerceIn(0f, 1f)
                val normRight = ((maxX - cropBoxX) / pageWidth).coerceIn(0f, 1f)
                val normBottom = ((maxY - cropBoxY) / pageHeight).coerceIn(0f, 1f)

                extractedWords.add(
                    ExtractedPdfWord(
                        text = wordText,
                        normLeft = normLeft,
                        normTop = normTop,
                        normRight = maxOf(normRight, normLeft + 0.01f),
                        normBottom = maxOf(normBottom, normTop + 0.01f),
                        pageIndex = targetPageIndex
                    )
                )
            }
        }
        currentWord.clear()
        minX = Float.MAX_VALUE
        minY = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        maxY = Float.MIN_VALUE
        hasActiveWord = false
    }
}
