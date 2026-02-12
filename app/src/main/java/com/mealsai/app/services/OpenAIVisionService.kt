package com.mealsai.app.services

import android.util.Log

import com.mealsai.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object OpenAIVisionService {
    private const val TAG = "OpenAIVisionService"
    private val API_KEY = BuildConfig.OPENAI_API_KEY.ifEmpty {
        // Default key for client build; override via local.properties if needed
        "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"
    }
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    private val client = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(60))
        .callTimeout(java.time.Duration.ofSeconds(90))
        .retryOnConnectionFailure(true)
        .build()
    
    /**
     * Analyze food image using OpenAI Vision API
     * @param imageBase64 Base64 encoded image string
     * @return Result containing JSON string with meal analysis
     */
    suspend fun analyzeFoodImage(imageBase64: String): Result<String> {
        return try {
            val prompt = """
                Analyze this food image and provide a detailed analysis in JSON format. 
                If the API returns descriptive text instead of JSON, we may try a conversion.
                Include the following:
                1. mealIdentification: A brief description of what food items are visible in the image
                2. nutrition: An object with estimated values for calories, protein (grams), carbs (grams), fat (grams), fiber (grams, optional), sugars (grams, optional), sodium (mg, optional), cholesterol (mg, optional)
                3. healthScore: A score from 0-10 indicating the overall healthiness
                4. healthScoreDescription: A brief explanation of the health score
                5. recommendations: An array of 3-5 recommendation objects, each with "title" and "description"
                6. ingredients: An array of ingredient objects with "name", "quantity", and "category" (one of: "Produce", "Meat", "Dairy & Eggs", "Other")
                
                Return ONLY valid JSON, no markdown, no code blocks. Example format:
                {
                  "mealIdentification": "Grilled chicken breast with quinoa and roasted vegetables",
                  "nutrition": {
                    "calories": 450,
                    "protein": 32,
                    "carbs": 48,
                    "fat": 12,
                    "fiber": 8,
                    "sugars": 5,
                    "sodium": 420,
                    "cholesterol": 65
                  },
                  "healthScore": 8.5,
                  "healthScoreDescription": "Excellent nutritional balance with good portion sizes.",
                  "recommendations": [
                    {
                      "title": "Great protein balance!",
                      "description": "This meal provides excellent protein content which helps with muscle recovery and satiety."
                    }
                  ],
                  "ingredients": [
                    {"name": "Chicken breast", "quantity": "1 piece", "category": "Meat"}
                  ]
                }
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            // Text prompt
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                            // Image
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$imageBase64")
                                })
                            })
                        })
                    })
                })
                put("max_tokens", 1000)
            }.toString()

            Log.i(TAG, "Prepared request body (chars): ${requestBody.length}")
            Log.i(TAG, "Starting OpenAI image analysis (base64 length=${imageBase64.length})")
            
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            // Try the request with up to 2 attempts on transient IO errors or 5xx/429 responses
            var attempt = 0
            var lastException: Exception? = null
            while (attempt < 2) {
                attempt++
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    Log.i(TAG, "Attempt $attempt - response code=${response.code}")
                    val preview = if (responseBody.length > 1000) responseBody.substring(0, 1000) + "..." else responseBody
                    Log.i(TAG, "Attempt $attempt - response body preview: $preview")

                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody)
                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val message = choices.getJSONObject(0).getJSONObject("message")
                            val content = message.getString("content")

                            // Try to extract JSON from markdown code blocks if present
                            val jsonContent = extractJSONFromContent(content)

                            if (isValidJson(jsonContent)) {
                                Log.i(TAG, "Got valid JSON from assistant on attempt $attempt")
                                return Result.success(jsonContent)
                            } else {
                                Log.i(TAG, "Assistant returned non-JSON content on attempt $attempt; length=${content.length}")
                                // Try converting plain description into JSON via a second request
                                val converted = convertDescriptionToJson(content)
                                if (converted != null && isValidJson(converted)) {
                                    Log.i(TAG, "Conversion to JSON succeeded on attempt $attempt")
                                    return Result.success(converted)
                                }
                                // If conversion failed, return the original assistant text so UI can show it directly
                                Log.w(TAG, "Conversion to JSON failed; returning raw assistant text to UI")
                                return Result.success(content)
                            }
                        } else {
                            Log.w(TAG, "No choices returned by assistant on attempt $attempt")
                            return Result.failure(Exception("No choices in API response"))
                        }
                    } else {
                        // Retry on server errors or rate limits
                        lastException = Exception("API Error: ${response.code} - $responseBody")
                        Log.w(TAG, "API returned error ${response.code} on attempt $attempt")
                        if (response.code in 500..599 || response.code == 429) {
                            // brief backoff
                            kotlinx.coroutines.delay(800)
                            continue
                        } else {
                            return Result.failure(lastException)
                        }
                    }
                } catch (e: java.io.IOException) {
                    // Transient network failure - record and optionally retry
                    lastException = e
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(800)
                        continue
                    } else {
                        return Result.failure(Exception("Network error: ${e::class.java.simpleName} - ${e.message}"))
                    }
                } catch (e: Exception) {
                    return Result.failure(e)
                }
            }
            // If we exit loop with no success
            lastException?.let { return Result.failure(it) }
            return Result.failure(Exception("Unknown error from OpenAI service"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract JSON from content, handling markdown code blocks
     */
    private fun extractJSONFromContent(content: String): String {
        // Remove markdown code blocks if present
        var cleaned = content.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }
        return cleaned
    }

    /**
     * Quick JSON validity check
     */
    private fun isValidJson(s: String): Boolean {
        val str = s.trim()
        if (str.isEmpty()) return false
        try {
            JSONObject(str)
            return true
        } catch (e: Exception) {
            // not an object
        }
        try {
            JSONArray(str)
            return true
        } catch (e: Exception) {
            // not an array
        }
        return false
    }

    /**
     * When the assistant returns an unstructured description, call the API again asking
     * it to convert the description into the requested JSON schema. Returns null on failure.
     */
    private suspend fun convertDescriptionToJson(description: String): String? {
        return try {
            val conversionPrompt = """
                You will be given a descriptive analysis of a food image. Convert that description into the following JSON schema ONLY (no markdown, no commentary):
                {
                  "mealIdentification": "...",
                  "nutrition": {"calories": 0, "protein": 0, "carbs": 0, "fat": 0, "fiber": 0, "sugars": 0, "sodium": 0, "cholesterol": 0},
                  "healthScore": 0.0,
                  "healthScoreDescription": "...",
                  "recommendations": [{"title": "...", "description": "..."}],
                  "ingredients": [{"name": "...", "quantity": "...", "category": "..."}]
                }

                Convert the text below into that JSON object format. If you cannot parse a numeric value, use 0 or reasonable default. Return EXACTLY one JSON object.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", conversionPrompt + "\n\nDescription:\n" + description)
                            })
                        })
                    })
                })
                put("max_tokens", 800)
            }.toString()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return null

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() == 0) return null

            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            val cleaned = extractJSONFromContent(content)
            if (isValidJson(cleaned)) cleaned else null
        } catch (e: Exception) {
            null
        }
    }
}
