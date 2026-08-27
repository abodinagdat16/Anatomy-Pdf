package com.example.data.anatomy

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service that searches and provides real anatomical images, diagrams, and cross-sections
 * directly inside the app without redirecting to external search engines.
 */
object AnatomyImageSearchService {
    private const val TAG = "AnatomyImageSearch"

    // In-memory cache for fast lookup
    private val imageCache = mutableMapOf<String, List<AnatomyImage>>()

    /**
     * Curated multi-image database for major anatomical structures
     */
    private val curatedStructureImageSets: Map<String, List<AnatomyImage>> = mapOf(
        "carotid" to listOf(
            AnatomyImage(
                title = "Carotid Arteries & Bifurcation",
                description = "Common, Internal, and External Carotid Arteries at the C4 bifurcation level.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
            ),
            AnatomyImage(
                title = "Carotid Sheath Fascial Compartment",
                description = "Cross-section of the neck showing CCA, Internal Jugular Vein, and Vagus Nerve in carotid sheath.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
            ),
            AnatomyImage(
                title = "Carotid Triangle Boundaries",
                description = "Surgical triangle of anterior neck bounded by SCM, Omohyoid, and Digastric muscles.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
            ),
            AnatomyImage(
                title = "Superficial Dissection of Right Carotid Neck",
                description = "Anterior neck neurovascular structures and branching patterns.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Gray557.png/800px-Gray557.png"
            )
        ),
        "external_carotid" to listOf(
            AnatomyImage(
                title = "External Carotid Artery & 8 Branches",
                description = "Overview of Superior Thyroid, Lingual, Facial, Occipital, Maxillary, and Superficial Temporal arteries.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
            ),
            AnatomyImage(
                title = "Facial and Lingual Arterial Branches",
                description = "Branches of ECA supplying the oral cavity, tongue, and face.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Gray514.png/800px-Gray514.png"
            ),
            AnatomyImage(
                title = "Maxillary Artery & Infratemporal Fossa",
                description = "Terminal branch of ECA entering infratemporal fossa to give Middle Meningeal Artery.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Gray512.png/800px-Gray512.png"
            )
        ),
        "internal_carotid" to listOf(
            AnatomyImage(
                title = "Internal Carotid Artery & Cerebral Circulation",
                description = "Ascends without cervical branches, enters carotid canal and supplies the brain.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Gray516.png/800px-Gray516.png"
            ),
            AnatomyImage(
                title = "Circle of Willis & Intracranial Arteries",
                description = "Anastomosis formed by Internal Carotid, Anterior Cerebral, Middle Cerebral, and Posterior Communicating.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/Circle_of_Willis_en.svg/800px-Circle_of_Willis_en.svg.png"
            ),
            AnatomyImage(
                title = "Carotid Siphon in Cavernous Sinus",
                description = "S-shaped intra-cavernous segment of the Internal Carotid Artery.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
            )
        ),
        "vagus" to listOf(
            AnatomyImage(
                title = "Vagus Nerve (CN X) Cervical Course",
                description = "Descends in posterior groove of carotid sheath between CCA and IJV.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Gray791.png/800px-Gray791.png"
            ),
            AnatomyImage(
                title = "Recurrent Laryngeal Nerve & Larynx",
                description = "Branch of Vagus looping around Subclavian (Right) / Aortic Arch (Left) to supply vocal cords.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
            )
        ),
        "jugular" to listOf(
            AnatomyImage(
                title = "Internal Jugular Vein & Neck Venous Drainage",
                description = "Large lateral venous channel within carotid sheath draining sigmoid sinus.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Gray557.png/800px-Gray557.png"
            ),
            AnatomyImage(
                title = "External Jugular & Superficial Neck Veins",
                description = "Superficial vein crossing obliquely across SCM muscle.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
            )
        ),
        "brachial" to listOf(
            AnatomyImage(
                title = "Brachial Plexus Schematic Diagram",
                description = "Roots (C5-T1), Trunks (Upper, Middle, Lower), Divisions, Cords (Lateral, Posterior, Medial), and Branches.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/Brachial_plexus_color.svg/800px-Brachial_plexus_color.svg.png"
            ),
            AnatomyImage(
                title = "Upper Extremity Nerves Distribution",
                description = "Musculocutaneous, Median, Ulnar, Radial, and Axillary nerves.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Gray791.png/800px-Gray791.png"
            )
        ),
        "circle_of_willis" to listOf(
            AnatomyImage(
                title = "Circle of Willis Complete Atlas",
                description = "Arterial circle formed by ACA, ACom, ICA, PCom, PCA, and Basilar Artery.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/Circle_of_Willis_en.svg/800px-Circle_of_Willis_en.svg.png"
            ),
            AnatomyImage(
                title = "Base of Brain & Arterial Circle",
                description = "Topographical relationships to optic chiasm, pituitary, and brainstem.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Gray516.png/800px-Gray516.png"
            )
        )
    )

    /**
     * Searches Google Images results directly and fetches anatomical diagrams for the query.
     */
    suspend fun searchAnatomyImages(
        query: String,
        limit: Int = 6
    ): List<AnatomyImage> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val cacheKey = cleanQuery.lowercase()
        imageCache[cacheKey]?.let { return@withContext it }

        val results = mutableListOf<AnatomyImage>()

        // 1. Fetch real Google Image Search results directly
        val googleResults = fetchGoogleImages(cleanQuery, limit)
        results.addAll(googleResults)

        // 2. If Google search results are fewer than desired, add curated multi-plate atlas images
        if (results.size < limit) {
            for ((key, list) in curatedStructureImageSets) {
                if (cacheKey.contains(key) || key.contains(cacheKey)) {
                    for (img in list) {
                        if (results.none { it.imageUrl == img.imageUrl }) {
                            results.add(img)
                        }
                    }
                    break
                }
            }
        }

        // 3. Query Wikipedia / Wikimedia API for additional authentic diagrams
        if (results.size < limit) {
            try {
                val encodedTitle = URLEncoder.encode(cleanQuery.replace(" ", "_"), "UTF-8")
                val apiUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=$encodedTitle&prop=pageimages|images&piprop=original|thumbnail&pithumbsize=1000&format=json"

                val url = URL(apiUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "MedicalAnatomyAtlas/2.0 (Android Medical App; dev@aistudio.app)")
                    connectTimeout = 4000
                    readTimeout = 4000
                }

                if (connection.responseCode == 200) {
                    val text = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val json = JSONObject(text)
                    val pagesObj = json.optJSONObject("query")?.optJSONObject("pages")
                    if (pagesObj != null) {
                        val keys = pagesObj.keys()
                        while (keys.hasNext()) {
                            val pageKey = keys.next()
                            if (pageKey != "-1") {
                                val page = pagesObj.getJSONObject(pageKey)
                                val pageTitle = page.optString("title", cleanQuery)
                                val original = page.optJSONObject("original")
                                val thumbnail = page.optJSONObject("thumbnail")
                                val imageUrl = original?.optString("source") ?: thumbnail?.optString("source")

                                if (!imageUrl.isNullOrBlank() && results.none { it.imageUrl == imageUrl }) {
                                    results.add(
                                        AnatomyImage(
                                            title = "$pageTitle Atlas Diagram",
                                            description = "High-yield medical illustration for $pageTitle.",
                                            imageUrl = imageUrl
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Wikipedia direct API search failed for $cleanQuery", e)
            }
        }

        // 4. Fallback curated anatomical plate if no image found
        if (results.isEmpty()) {
            val fallbackUrl = getCuratedFallbackForQuery(cleanQuery)
            results.add(
                AnatomyImage(
                    title = "$cleanQuery Anatomical Atlas",
                    description = "High-yield schematic and anatomical orientation for $cleanQuery.",
                    imageUrl = fallbackUrl
                )
            )
        }

        val finalResults = results.distinctBy { it.imageUrl }.take(limit)
        imageCache[cacheKey] = finalResults
        return@withContext finalResults
    }

    /**
     * Directly queries Google Images and parses image URLs from search results HTML
     */
    private fun fetchGoogleImages(query: String, limit: Int): List<AnatomyImage> {
        val results = mutableListOf<AnatomyImage>()
        try {
            val encodedQuery = URLEncoder.encode("$query anatomy diagram", "UTF-8")
            val searchUrl = "https://www.google.com/search?tbm=isch&q=$encodedQuery&safe=active"

            val connection = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == 200) {
                val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }

                // Extract Google encrypted-tbn thumbnail URLs
                val tbnRegex = Regex("""https://encrypted-tbn0\.gstatic\.com/images\?q=[a-zA-Z0-9_\-:]+""")
                val tbnMatches = tbnRegex.findAll(html).map { it.value }.distinct().toList()

                // Extract full-res image URLs from Google Images JSON data
                val fullImgRegex = Regex(""""(https?://[^"\\]+?\.(?:jpg|jpeg|png|webp))"""", RegexOption.IGNORE_CASE)
                val fullMatches = fullImgRegex.findAll(html)
                    .map { it.groupValues[1] }
                    .filter { !it.contains("gstatic.com") && !it.contains("google.com") && !it.contains("logo") }
                    .distinct()
                    .toList()

                // Extract Alt descriptions
                val titleRegex = Regex("""alt="([^"]*?)"""")
                val titles = titleRegex.findAll(html)
                    .map { it.groupValues[1] }
                    .filter { it.isNotBlank() && !it.contains("Google", true) }
                    .toList()

                val availableCount = maxOf(tbnMatches.size, fullMatches.size)
                for (i in 0 until minOf(availableCount, limit)) {
                    val imageUrl = fullMatches.getOrNull(i) ?: tbnMatches.getOrNull(i)
                    if (!imageUrl.isNullOrBlank()) {
                        val title = titles.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "$query (Google Image Result)"
                        results.add(
                            AnatomyImage(
                                title = title,
                                description = "Google Image Search diagram for $query",
                                imageUrl = imageUrl
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Image search failed for $query", e)
        }
        return results
    }

    private fun getCuratedFallbackForQuery(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("artery") || q.contains("aorta") || q.contains("carotid") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
            q.contains("vein") || q.contains("jugular") || q.contains("cava") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Gray557.png/800px-Gray557.png"
            q.contains("nerve") || q.contains("vagus") || q.contains("plexus") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Gray791.png/800px-Gray791.png"
            q.contains("sheath") || q.contains("fascia") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
            q.contains("brain") || q.contains("circle") || q.contains("cerebr") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/Circle_of_Willis_en.svg/800px-Circle_of_Willis_en.svg.png"
            q.contains("heart") || q.contains("ventricle") || q.contains("atrium") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Diagram_of_the_human_heart_%28cropped%29.svg/800px-Diagram_of_the_human_heart_%28cropped%29.svg.png"
            q.contains("triangle") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
            else ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
        }
    }
}

