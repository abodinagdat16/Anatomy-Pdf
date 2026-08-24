package com.example.data.gemini

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.anatomy.AnatomyImage
import com.example.data.anatomy.AnatomyRelations
import com.example.data.anatomy.AnatomyStructure
import com.example.data.anatomy.BranchLink
import com.example.data.anatomy.StructureCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contextText: String? = null,
    val suggestedQuestions: List<String> = emptyList()
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val PREFS_NAME = "gemini_api_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    private var inMemoryApiKey: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getActiveApiKey(context: Context? = null): String {
        inMemoryApiKey?.let { if (it.isNotBlank()) return it }
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedKey = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
            if (savedKey.isNotBlank()) {
                inMemoryApiKey = savedKey
                return savedKey
            }
        }
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveCustomApiKey(context: Context, apiKey: String) {
        val cleanKey = apiKey.trim()
        inMemoryApiKey = cleanKey
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_API_KEY, cleanKey)
            .apply()
    }

    fun clearCustomApiKey(context: Context) {
        inMemoryApiKey = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_API_KEY)
            .apply()
    }

    fun getSavedCustomApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun hasCustomApiKey(context: Context): Boolean {
        return getSavedCustomApiKey(context).isNotBlank()
    }

    fun getApiKeyStatusDescription(context: Context): String {
        val customKey = getSavedCustomApiKey(context)
        if (customKey.isNotBlank()) {
            val masked = if (customKey.length > 8) "${customKey.take(4)}...${customKey.takeLast(4)}" else "••••••••"
            return "Active (Custom Key: $masked)"
        }
        val envKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (envKey.isNotBlank() && envKey != "MY_GEMINI_API_KEY") {
            return "Active (Default AI Studio Key)"
        }
        return "Not Configured"
    }

    suspend fun testApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) return@withContext Pair(false, "API Key cannot be empty.")
        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$key"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Say OK"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 5)
                })
            }
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Pair(true, "API Key is valid and connected successfully!")
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "HTTP error ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage ?: "Failed to connect to Google Gemini API"}")
        }
    }

    /**
     * Medical chat assistant with Gemini for medical students
     */
    suspend fun chatWithGemini(
        messages: List<ChatMessage>,
        medicalContext: String? = null,
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ Gemini API key is missing or not configured.\n\nPlease tap the ⚙️ Key icon in the top bar to enter your Gemini API key from Google AI Studio (aistudio.google.com)."
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val systemInstruction = """
                You are a world-class Anatomy & Medical Education AI Assistant for medical students (USMLE Step 1 / Medical Board Exam level).
                Provide accurate, high-yield, structured, and clinically relevant anatomical explanations.
                Always highlight:
                1. High-yield anatomical landmarks & relations (Anterior, Posterior, Medial, Lateral).
                2. Precise course, origins, and branches.
                3. Clinical correlations (syndromes, surgical landmarks, clinical traps, board exam pearls).
                4. Memorable mnemonics.
                Format your responses with clean Markdown, bullet points, bold anatomical keywords, and structured sections.
            """.trimIndent()

            val contentsArray = JSONArray()

            // Include medical context if present
            if (!medicalContext.isNullOrBlank()) {
                val contextObj = JSONObject()
                contextObj.put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", "Context from current lecture: \"$medicalContext\""))
                contextObj.put("parts", parts)
                contentsArray.put(contextObj)

                val ackObj = JSONObject()
                ackObj.put("role", "model")
                val ackParts = JSONArray()
                ackParts.put(JSONObject().put("text", "Understood. I have reviewed the passage and am ready to answer medical and anatomical questions regarding this topic."))
                ackObj.put("parts", ackParts)
                contentsArray.put(ackObj)
            }

            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("role", if (msg.isUser) "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.text))
                msgObj.put("parts", parts)
                contentsArray.put(msgObj)
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.95)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} $responseBody")
                return@withContext "Error from Gemini API (${response.code}). Please check your API key and connection."
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            text ?: "No response generated."
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini", e)
            "Failed to communicate with Gemini AI: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Dynamically generates a structured Anatomy deep dive card for any anatomical term
     */
    suspend fun generateAnatomyCard(term: String, context: Context? = null): AnatomyStructure? = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val prompt = """
                Generate a structured JSON anatomy breakdown for medical students studying: "$term".
                Respond ONLY with a valid raw JSON object matching this schema (do not wrap in markdown code blocks or backticks):
                {
                  "name": "Full English Anatomical Name",
                  "latinName": "Latin Terminology",
                  "category": "ARTERY" or "VEIN" or "NERVE" or "MUSCLE" or "FASCIA_SHEATH" or "ANATOMICAL_SPACE" or "ORGAN" or "BONE",
                  "origin": "Embryological or anatomical origin",
                  "termination": "Destination or bifurcation point",
                  "definition": "Clear concise 1-2 sentence definition",
                  "course": "Detailed trajectory, compartments, and path",
                  "anteriorRelations": ["relation 1", "relation 2"],
                  "posteriorRelations": ["relation 1", "relation 2"],
                  "medialRelations": ["relation 1", "relation 2"],
                  "lateralRelations": ["relation 1", "relation 2"],
                  "branches": [
                    {"name": "Branch or Tributary name", "description": "Quick description"}
                  ],
                  "clinicalCorrelations": [
                    "High yield clinical pearl 1",
                    "High yield clinical pearl 2"
                  ],
                  "mnemonics": "Board exam mnemonic",
                  "highYieldSummary": "One sentence high yield takeaway"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext null

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val candidateText = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: return@withContext null

            val cleanJson = candidateText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)

            val catStr = obj.optString("category", "ORGAN")
            val category = try {
                StructureCategory.valueOf(catStr)
            } catch (e: Exception) {
                StructureCategory.ORGAN
            }

            val anteriorList = mutableListOf<String>()
            obj.optJSONArray("anteriorRelations")?.let { arr ->
                for (i in 0 until arr.length()) anteriorList.add(arr.getString(i))
            }

            val posteriorList = mutableListOf<String>()
            obj.optJSONArray("posteriorRelations")?.let { arr ->
                for (i in 0 until arr.length()) posteriorList.add(arr.getString(i))
            }

            val medialList = mutableListOf<String>()
            obj.optJSONArray("medialRelations")?.let { arr ->
                for (i in 0 until arr.length()) medialList.add(arr.getString(i))
            }

            val lateralList = mutableListOf<String>()
            obj.optJSONArray("lateralRelations")?.let { arr ->
                for (i in 0 until arr.length()) lateralList.add(arr.getString(i))
            }

            val branchList = mutableListOf<BranchLink>()
            obj.optJSONArray("branches")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val bObj = arr.optJSONObject(i)
                    if (bObj != null) {
                        branchList.add(BranchLink(
                            name = bObj.optString("name"),
                            description = bObj.optString("description")
                        ))
                    }
                }
            }

            val clinicalList = mutableListOf<String>()
            obj.optJSONArray("clinicalCorrelations")?.let { arr ->
                for (i in 0 until arr.length()) clinicalList.add(arr.getString(i))
            }

            val id = term.lowercase().replace(" ", "_")

            AnatomyStructure(
                id = id,
                name = obj.optString("name", term),
                latinName = obj.optString("latinName", ""),
                category = category,
                origin = obj.optString("origin", ""),
                termination = obj.optString("termination", ""),
                definition = obj.optString("definition", ""),
                course = obj.optString("course", ""),
                relations = AnatomyRelations(
                    anterior = anteriorList,
                    posterior = posteriorList,
                    medial = medialList,
                    lateral = lateralList
                ),
                branches = branchList,
                clinicalCorrelations = clinicalList,
                mnemonics = obj.optString("mnemonics", ""),
                highYieldSummary = obj.optString("highYieldSummary", ""),
                images = listOf(
                    AnatomyImage(
                        title = "$term Medical Reference",
                        description = "High-yield anatomical structure illustration and schematics.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating anatomy card", e)
            null
        }
    }
}
