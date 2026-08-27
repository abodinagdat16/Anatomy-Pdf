package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfDocumentManager {
    private const val TAG = "PdfDocumentManager"

    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentPdfRenderer: PdfRenderer? = null
    private val rendererMutex = Any()
    private val pageBitmapCache = LruCache<Int, Bitmap>(64)
    private val documentTextCache = mutableMapOf<String, List<ExtractedPageResult>>()

    private var activeContext: Context? = null
    private var activeDocumentItem: PdfDocumentItem? = null

    /**
     * Resolves human-readable document name from ContentProvider or File Uri
     */
    fun resolveDisplayName(context: Context, uri: Uri): String {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            return name
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val name = uri.lastPathSegment
                if (!name.isNullOrBlank()) {
                    return name
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve display name for uri: $uri", e)
        }
        return uri.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
    }

    /**
     * Safely returns a local File instance for any PDF (copying content URIs to local cache)
     */
    fun getLocalFileForDocument(context: Context, item: PdfDocumentItem): File {
        val safeId = item.id.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val cachedFile = File(context.cacheDir, "doc_$safeId.pdf")

        if (item.uri != null) {
            try {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying URI to local cache: ${item.uri}", e)
            }
        }
        return cachedFile
    }

    /**
     * Opens a PDF file and prepares the PdfRenderer
     */
    suspend fun openPdf(context: Context, item: PdfDocumentItem): Int = withContext(Dispatchers.IO) {
        synchronized(rendererMutex) {
            activeContext = context.applicationContext
            activeDocumentItem = item
            closeCurrentRenderer()
            pageBitmapCache.evictAll()
            try {
                val file = getLocalFileForDocument(context, item)
                if (file.exists() && file.length() > 0) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    currentFileDescriptor = pfd
                    currentPdfRenderer = PdfRenderer(pfd)
                    val count = currentPdfRenderer?.pageCount ?: 0
                    Log.d(TAG, "Successfully opened PDF ${item.title} with $count pages")
                    return@withContext count
                } else {
                    Log.e(TAG, "File does not exist or is empty: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening PDF: ${item.title}", e)
            }
            return@withContext 0
        }
    }

    /**
     * Renders a specific page to a high-res bitmap with caching
     */
    suspend fun renderPage(pageIndex: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(rendererMutex) {
            val cached = pageBitmapCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }

            // Auto-reopen if renderer was cleared or closed
            if (currentPdfRenderer == null && activeContext != null && activeDocumentItem != null) {
                try {
                    val file = getLocalFileForDocument(activeContext!!, activeDocumentItem!!)
                    if (file.exists() && file.length() > 0) {
                        currentFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        currentPdfRenderer = PdfRenderer(currentFileDescriptor!!)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-reopen failed", e)
                }
            }

            val renderer = currentPdfRenderer
            if (renderer != null && pageIndex in 0 until renderer.pageCount) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val targetWidth = (page.width * 1.6f).toInt().coerceIn(600, 1400)
                    val targetHeight = (page.height * 1.6f).toInt().coerceIn(800, 2000)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    pageBitmapCache.put(pageIndex, bitmap)
                    return@withContext bitmap
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering page $pageIndex from renderer", e)
                }
            }

            // Fallback: Generate empty clean page if rendering fails
            val docId = activeDocumentItem?.id ?: "unknown"
            val fallbackBmp = generateFallbackPageBitmap(docId, pageIndex)
            pageBitmapCache.put(pageIndex, fallbackBmp)
            return@withContext fallbackBmp
        }
    }

    private fun generateFallbackPageBitmap(docId: String, pageIndex: Int): Bitmap {
        val width = 1000
        val height = 1414
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val topBarPaint = Paint().apply { color = Color.rgb(26, 115, 232) }
        canvas.drawRect(0f, 0f, width.toFloat(), 18f, topBarPaint)

        val textPaint = Paint().apply {
            color = Color.rgb(60, 64, 67)
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Page ${pageIndex + 1}", 60f, 100f, textPaint)
        return bmp
    }

    /**
     * Loads all pages of a document and extracts real text & word coordinates
     */
    suspend fun loadAllDocumentPages(
        context: Context,
        item: PdfDocumentItem,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<PdfPageData> = withContext(Dispatchers.IO) {
        val file = getLocalFileForDocument(context, item)
        val total = openPdf(context, item)

        val extractedPages = try {
            if (file.exists() && file.length() > 0) {
                PdfBoxHelper.extractAllPages(context, file)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting pages with PDFBox for ${item.title}", e)
            emptyList()
        }

        documentTextCache[item.id] = extractedPages

        val count = if (total > 0) total else maxOf(extractedPages.size, item.pageCount, 1)
        val result = mutableListOf<PdfPageData>()

        for (i in 0 until count) {
            val bitmap = if (i < 3) renderPage(i) else pageBitmapCache.get(i)
            val extracted = extractedPages.getOrNull(i)
            val pageText = extracted?.fullText ?: ""
            val words = if (extracted != null && extracted.words.isNotEmpty()) {
                extracted.words
            } else if (pageText.isNotBlank()) {
                PdfBoxHelper.createFallbackWordBoxes(pageText, i)
            } else {
                emptyList()
            }
            val keyTerms = extractKeyTermsFromPage(pageText)

            result.add(
                PdfPageData(
                    pageIndex = i,
                    totalPages = count,
                    bitmap = bitmap,
                    text = pageText,
                    words = words,
                    keyTerms = keyTerms
                )
            )
            onProgress?.invoke(i + 1, count)
        }
        return@withContext result
    }

    private fun extractKeyTermsFromPage(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val knownTerms = listOf(
            "Common Carotid Artery", "Internal Carotid Artery", "External Carotid Artery",
            "Carotid Sheath", "Carotid Triangle", "Internal Jugular Vein", "Vagus Nerve",
            "Circle of Willis", "Anterior Cerebral Artery", "Middle Cerebral Artery",
            "Anterior Communicating Artery", "Posterior Communicating Artery", "Basilar Artery",
            "Vertebral Arteries", "Brachial Plexus", "Musculocutaneous Nerve", "Median Nerve",
            "Ulnar Nerve", "Radial Nerve", "Axillary Nerve", "Superior Thyroid Artery",
            "Lingual Artery", "Facial Artery", "Maxillary Artery", "Superficial Temporal Artery",
            "Carotid Sinus", "Carotid Body", "Subclavian Artery", "Aorta", "Trachea", "Esophagus",
            "Thyroid Gland", "Larynx", "Pharynx", "Sternocleidomastoid", "Omohyoid", "Digastric",
            "Hypoglossal Nerve", "Accessory Nerve", "Glossopharyngeal Nerve", "Facial Nerve",
            "Trigeminal Nerve", "Optic Chiasm", "Cavernous Sinus", "Jugular Foramen"
        )
        val matched = knownTerms.filter { term -> text.contains(term, ignoreCase = true) }
        return matched.take(6)
    }

    /**
     * Returns the structured text corresponding to a page
     */
    fun getPageText(docId: String, pageIndex: Int): String {
        return documentTextCache[docId]?.getOrNull(pageIndex)?.fullText ?: ""
    }

    fun closeCurrentRenderer() {
        try {
            currentPdfRenderer?.close()
            currentPdfRenderer = null
            currentFileDescriptor?.close()
            currentFileDescriptor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing renderer", e)
        }
    }
}
