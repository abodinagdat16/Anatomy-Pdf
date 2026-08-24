package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfDocumentManager {
    private const val TAG = "PdfDocumentManager"

    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentPdfRenderer: PdfRenderer? = null

    val sampleLectures = listOf(
        PdfDocumentItem(
            id = "lecture_carotid_triangle",
            title = "Head & Neck: Carotid Triangles & Great Vessels",
            subtitle = "Common Carotid Artery, Carotid Sheath, Bifurcation & Relations",
            pageCount = 3,
            isPreset = true,
            topicTag = "Head & Neck"
        ),
        PdfDocumentItem(
            id = "lecture_circle_of_willis",
            title = "Neuroanatomy: Circle of Willis & Cerebral Arteries",
            subtitle = "Internal Carotid Artery, Vertebrobasilar System & Aneurysms",
            pageCount = 2,
            isPreset = true,
            topicTag = "Neuroanatomy"
        ),
        PdfDocumentItem(
            id = "lecture_upper_limb_brachial",
            title = "Upper Limb: Brachial Plexus & Axillary Region",
            subtitle = "Roots, Trunks, Divisions, Cords & Neurovascular Relations",
            pageCount = 2,
            isPreset = true,
            topicTag = "Upper Limb"
        )
    )

    private val presetLecturePages = mapOf(
        "lecture_carotid_triangle" to listOf(
            """
            CLINICAL ANATOMY LECTURE SERIES
            Topic: Anterior Neck & The Carotid Triangle
            
            1. The Common Carotid Artery (CCA)
            The Common Carotid Artery is the principal systemic blood supplier to the head, neck, and intracranial structures. The Right CCA arises from the brachiocephalic trunk behind the right sternoclavicular joint. The Left CCA arises directly from the arch of the aorta within the superior mediastinum, making it longer and more proximal.
            
            2. The Carotid Sheath & Spatial Relations
            Ascending vertically within the neck, the Common Carotid Artery is invested inside the Carotid Sheath, a condensation of deep cervical fascia.
            Within the sheath:
            • Medial: Common Carotid Artery (inferiorly) and Internal Carotid Artery (superiorly).
            • Lateral: Internal Jugular Vein (IJV).
            • Posterior: Vagus Nerve (Cranial Nerve X), situated in the posterior groove between artery and vein.
            • Posterior to the sheath (outside): Cervical Sympathetic Trunk.
            • Anterior wall: Ansa Cervicalis embedded on its superficial surface.
            
            3. The Carotid Bifurcation
            At the level of the upper border of the thyroid cartilage (vertebral level C3–C4 disc), the CCA divides into two terminal vessels:
            • Internal Carotid Artery (ICA): Ascends without giving ANY branches in the neck.
            • External Carotid Artery (ECA): Gives off eight major branches supplying the facial, lingual, and maxillary territories.
            
            Key Baroreceptors and Chemoreceptors:
            The Carotid Sinus (dilation at ICA origin, CN IX innervation) monitors blood pressure.
            The Carotid Body (chemoreceptor at the bifurcation) monitors arterial PaO2 and PaCO2.
            """.trimIndent(),

            """
            CLINICAL ANATOMY LECTURE SERIES: CAROTID SYSTEM (CONT.)
            
            4. The Carotid Triangle Boundaries & Surgical Importance
            The Carotid Triangle is an anterior cervical subdivision crucial for vascular surgery:
            • Superior: Posterior belly of Digastric muscle and Stylohyoid.
            • Anteroinferior: Superior belly of Omohyoid muscle.
            • Posterior: Anterior border of Sternocleidomastoid (SCM).
            • Floor: Hyoglossus, Thyrohyoid, and Middle & Inferior Pharyngeal Constrictors.
            
            Neurovascular Contents:
            1. Common Carotid Artery bifurcation into ICA and ECA.
            2. Branches of ECA: Superior Thyroid Artery, Lingual Artery, Facial Artery, Ascending Pharyngeal Artery, Occipital Artery.
            3. Internal Jugular Vein and tributaries (Common Facial Vein, Lingual Vein).
            4. Vagus Nerve (CN X), Hypoglossal Nerve (CN XII), and Accessory Nerve (CN XI).
            
            5. Clinical Correlations:
            • Carotid Endarterectomy (CEA): Surgical excision of atheromatous plaque at the bifurcation to prevent embolic ischemic stroke and TIA.
            • Carotid Sinus Hypersensitivity: Exaggerated vagal bradycardia and syncope upon neck collar compression.
            • Carotid Pulse: Palpated in the carotid triangle anterior to SCM at the level of the cricoid cartilage (C6 - Chassaignac's tubercle).
            """.trimIndent(),

            """
            CLINICAL ANATOMY LECTURE SERIES: ARTERIAL BRANCHING
            
            6. External Carotid Artery (ECA) Branches
            The ECA provides 8 branches categorized by emergence:
            • Anterior Branches:
              - Superior Thyroid Artery (gives Superior Laryngeal Artery)
              - Lingual Artery (passes deep to Hyoglossus to supply the tongue)
              - Facial Artery (winds around inferior border of mandible)
            • Posterior Branches:
              - Occipital Artery (courses in occipital groove)
              - Posterior Auricular Artery
            • Medial Branch:
              - Ascending Pharyngeal Artery
            • Terminal Branches:
              - Maxillary Artery (enters infratemporal fossa; gives Middle Meningeal Artery)
              - Superficial Temporal Artery (palpable anterior to the tragus)
            
            Board Exam Mnemonic:
            "Some Anatomists Like Fucking, Others Prefer Many Students"
            (Superior thyroid, Ascending pharyngeal, Lingual, Facial, Occipital, Posterior auricular, Maxillary, Superficial temporal).
            """.trimIndent()
        ),

        "lecture_circle_of_willis" to listOf(
            """
            NEUROVASCULAR ANATOMY: THE CIRCLE OF WILLIS
            
            1. Overview & Location
            The Circle of Willis (circulus arteriosus cerebri) is a polygonal anastomotic vascular ring situated in the interpeduncular cistern at the base of the brain. It establishes vital collateral circulation between the anterior (internal carotid) and posterior (vertebrobasilar) circulations.
            
            2. Components of the Circle:
            • Anterior Circulation (Internal Carotid Artery):
              - Internal Carotid Artery (ICA): Enters carotid canal, traverses cavernous sinus, gives Anterior Cerebral Artery (ACA) and Middle Cerebral Artery (MCA).
              - Anterior Cerebral Artery (ACA): Supplies medial hemisphere (lower extremity motor & sensory cortex).
              - Anterior Communicating Artery (ACom): Bridges the left and right ACAs.
            • Posterior Circulation (Vertebrobasilar System):
              - Vertebral Arteries merge to form the Basilar Artery.
              - Basilar Artery bifurcates into the Posterior Cerebral Arteries (PCA).
              - Posterior Communicating Artery (PCom): Connects the ICA to the PCA.
            """.trimIndent(),

            """
            NEUROVASCULAR ANATOMY: CLINICAL CORRELATIONS
            
            3. Saccular (Berry) Aneurysms
            • Anterior Communicating Artery (ACom): Most frequent site (~85% of anterior circle aneurysms). Compression can cause bitemporal hemianopsia (optic chiasm compression).
            • Posterior Communicating Artery (PCom): Second most frequent site. Aneurysm enlargement frequently compresses Cranial Nerve III (Oculomotor Nerve), producing ipsilateral ptosis, 'down and out' pupil, and fixed mydriasis.
            
            4. Subarachnoid Hemorrhage (SAH)
            Rupture of berry aneurysms spills arterial blood into the subarachnoid space, causing sudden catastrophic 'thunderclap' headache ('worst headache of my life'), nuchal rigidity, and xanthochromia on CSF analysis.
            """.trimIndent()
        ),

        "lecture_upper_limb_brachial" to listOf(
            """
            UPPER LIMB ANATOMY: THE BRACHIAL PLEXUS
            
            1. Organization & Segments
            The Brachial Plexus supplies motor and sensory innervation to the entire upper extremity.
            Mnemonic: "Roots, Trunks, Divisions, Cords, Branches" -> "Remember To Drink Cold Beer".
            
            • Roots: Ventral rami of spinal nerves C5, C6, C7, C8, T1.
            • Trunks (in posterior triangle of neck):
              - Upper Trunk: C5 + C6
              - Middle Trunk: C7
              - Lower Trunk: C8 + T1
            • Divisions (behind the clavicle):
              - Each trunk splits into Anterior and Posterior divisions.
            • Cords (arranged around the Axillary Artery):
              - Lateral Cord: Anterior divisions of Upper and Middle trunks (C5–C7).
              - Medial Cord: Anterior division of Lower trunk (C8–T1).
              - Posterior Cord: Posterior divisions of ALL three trunks (C5–T1).
            """.trimIndent(),

            """
            UPPER LIMB ANATOMY: TERMINAL NERVES & CLINICAL INJURIES
            
            2. Terminal Nerve Branches:
            • Musculocutaneous Nerve: Lateral cord (C5–C7) -> Biceps brachii, Coracobrachialis, Brachialis.
            • Median Nerve: Medial + Lateral cords (C5–T1) -> Forearm flexors, thenar muscles.
            • Ulnar Nerve: Medial cord (C8–T1) -> Intrinsic hand muscles, hypothenar.
            • Radial Nerve: Posterior cord (C5–T1) -> Triceps, wrist extensors (Wrist Drop in radial palsy).
            • Axillary Nerve: Posterior cord (C5–C6) -> Deltoid, Teres minor (Surgical neck humerus fracture).
            
            3. Clinical Plexopathies:
            • Erb-Duchenne Palsy ('Waiter's Tip'): Upper trunk (C5–C6) traction injury during difficult delivery.
            • Klumpke Palsy ('Claw Hand'): Lower trunk (C8–T1) injury from sudden upward pull on arm.
            """.trimIndent()
        )
    )

    /**
     * Initializes and writes physical PDF files to local disk for the preset lectures
     */
    suspend fun ensurePresetPdfFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            for ((lectureId, pages) in presetLecturePages) {
                val file = File(context.filesDir, "$lectureId.pdf")
                if (!file.exists()) {
                    createPdfFileOnDisk(file, pages, lectureId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating preset PDFs", e)
        }
    }

    private fun createPdfFileOnDisk(file: File, pages: List<String>, title: String) {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 115, 232) // Google Blue
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.rgb(32, 33, 36)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(60, 64, 67)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val highlightPaint = Paint().apply {
            color = Color.rgb(218, 232, 252)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(218, 220, 224)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        for (i in pages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // White background
            canvas.drawColor(Color.WHITE)

            // Top decorative bar (Google Medical theme)
            val topBarPaint = Paint().apply { color = Color.rgb(26, 115, 232) }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, topBarPaint)

            // Header box
            canvas.drawRect(36f, 30f, pageWidth - 36f, 75f, highlightPaint)
            canvas.drawRect(36f, 30f, pageWidth - 36f, 75f, borderPaint)

            canvas.drawText("MEDDOC ANATOMY • CLINICAL STUDY ATLAS", 48f, 52f, titlePaint)
            val subtitlePaint = Paint().apply {
                color = Color.rgb(95, 99, 104)
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText("Interactive Medical Student Lecture Notes • Page ${i + 1} of ${pages.size}", 48f, 68f, subtitlePaint)

            // Content
            val rawText = pages[i]
            val lines = rawText.split("\n")
            var currentY = 100f
            val startX = 40f
            val maxLineWidth = pageWidth - 80f

            for (line in lines) {
                if (line.isBlank()) {
                    currentY += 12f
                    continue
                }

                val isHeading = line.startsWith("1.") || line.startsWith("2.") ||
                                line.startsWith("3.") || line.startsWith("4.") ||
                                line.startsWith("5.") || line.startsWith("6.") ||
                                line.startsWith("Topic:") || line.startsWith("CLINICAL")

                val paintToUse = if (isHeading) headingPaint else textPaint

                // Word wrapping for long lines
                val words = line.split(" ")
                var lineBuffer = StringBuilder()

                for (word in words) {
                    val testLine = if (lineBuffer.isEmpty()) word else "$lineBuffer $word"
                    val measure = paintToUse.measureText(testLine)
                    if (measure > maxLineWidth) {
                        canvas.drawText(lineBuffer.toString(), startX, currentY, paintToUse)
                        currentY += if (isHeading) 18f else 15f
                        lineBuffer = StringBuilder(word)
                    } else {
                        lineBuffer = StringBuilder(testLine)
                    }
                }
                if (lineBuffer.isNotEmpty()) {
                    canvas.drawText(lineBuffer.toString(), startX, currentY, paintToUse)
                    currentY += if (isHeading) 20f else 16f
                }
            }

            // Footer
            val footerPaint = Paint().apply {
                color = Color.rgb(154, 160, 166)
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawLine(36f, pageHeight - 40f, pageWidth - 36f, pageHeight - 40f, borderPaint)
            canvas.drawText("Tap any structure (e.g. Common Carotid Artery, Carotid Sheath) or select text for Instant Anatomy & Gemini AI.", 40f, pageHeight - 25f, footerPaint)
            canvas.drawText("Page ${i + 1}", pageWidth - 70f, pageHeight - 25f, footerPaint)

            document.finishPage(page)
        }

        val out = FileOutputStream(file)
        document.writeTo(out)
        out.flush()
        out.close()
        document.close()
    }

    /**
     * Loads a PDF from preset ID or URI and prepares the PdfRenderer
     */
    suspend fun openPdf(context: Context, item: PdfDocumentItem): Int = withContext(Dispatchers.IO) {
        closeCurrentRenderer()
        try {
            val pfd: ParcelFileDescriptor? = if (item.uri != null) {
                context.contentResolver.openFileDescriptor(item.uri, "r")
            } else {
                val file = File(context.filesDir, "${item.id}.pdf")
                if (!file.exists()) {
                    ensurePresetPdfFiles(context)
                }
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            if (pfd != null) {
                currentFileDescriptor = pfd
                currentPdfRenderer = PdfRenderer(pfd)
                return@withContext currentPdfRenderer?.pageCount ?: 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF", e)
        }
        return@withContext 0
    }

    /**
     * Renders a specific page to a high-res bitmap
     */
    suspend fun renderPage(pageIndex: Int, densityDpi: Int = 300): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = currentPdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        try {
            val page = renderer.openPage(pageIndex)
            // 2x scaling for crisp reading and zoom
            val width = (page.width * 2).coerceAtLeast(800)
            val height = (page.height * 2).coerceAtLeast(1100)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return@withContext bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page $pageIndex", e)
            null
        }
    }

    /**
     * Loads and renders all pages of a document as a continuous list
     */
    suspend fun loadAllDocumentPages(
        context: Context,
        item: PdfDocumentItem
    ): List<PdfPageData> = withContext(Dispatchers.IO) {
        val total = openPdf(context, item)
        val count = if (total > 0) total else item.pageCount
        val result = mutableListOf<PdfPageData>()

        for (i in 0 until count) {
            val bitmap = renderPage(i)
            val text = getPageText(item.id, i)
            val keyTerms = extractKeyTermsFromPage(item.id, i, text)
            result.add(
                PdfPageData(
                    pageIndex = i,
                    totalPages = count,
                    bitmap = bitmap,
                    text = text,
                    keyTerms = keyTerms
                )
            )
        }
        return@withContext result
    }

    private fun extractKeyTermsFromPage(lectureId: String, pageIndex: Int, text: String): List<String> {
        val knownTerms = listOf(
            "Common Carotid Artery",
            "Carotid Sheath",
            "Carotid Triangle",
            "Internal Carotid Artery",
            "External Carotid Artery",
            "Internal Jugular Vein",
            "Vagus Nerve",
            "Circle of Willis",
            "Anterior Cerebral Artery",
            "Middle Cerebral Artery",
            "Anterior Communicating Artery",
            "Posterior Communicating Artery",
            "Basilar Artery",
            "Vertebral Arteries",
            "Brachial Plexus",
            "Musculocutaneous Nerve",
            "Median Nerve",
            "Ulnar Nerve",
            "Radial Nerve",
            "Axillary Nerve",
            "Superior Thyroid Artery",
            "Lingual Artery",
            "Facial Artery",
            "Maxillary Artery",
            "Superficial Temporal Artery",
            "Carotid Sinus",
            "Carotid Body"
        )
        val matched = knownTerms.filter { term -> text.contains(term, ignoreCase = true) }
        return if (matched.isNotEmpty()) matched.take(6) else knownTerms.take(4)
    }

    /**
     * Returns the structured text corresponding to a page
     */
    fun getPageText(lectureId: String, pageIndex: Int): String {
        val pages = presetLecturePages[lectureId]
        if (pages != null && pageIndex in pages.indices) {
            return pages[pageIndex]
        }
        return "Medical Anatomy Lecture Notes (Page ${pageIndex + 1})\nSelect any anatomical term to view instant definitions, courses, relations, and branches."
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
