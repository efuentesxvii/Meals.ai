package com.mealsai.app.services

import com.mealsai.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object OpenAIService {
    private val API_KEY = BuildConfig.OPENAI_API_KEY.ifEmpty {
        // Default key for client build; override via local.properties if needed
        "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"
    }
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    private val client = OkHttpClient()
    
    suspend fun generateMeal(prompt: String): Result<String> {
        return try {
            val requestBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 500)
            }.toString()
            
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content")
                Result.success(content)
            } else {
                Result.failure(Exception("API Error: ${response.code} - $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
