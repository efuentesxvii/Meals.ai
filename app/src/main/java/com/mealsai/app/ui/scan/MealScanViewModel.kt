package com.mealsai.app.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealsai.app.model.NutritionDetails
import com.mealsai.app.model.Recommendation
import com.mealsai.app.services.OpenAIVisionService
import com.mealsai.app.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class MealScanState {
    object Loading : MealScanState()
    data class Success(
        val mealIdentification: String,
        val nutrition: NutritionDetails?,
        val healthScore: Double,
        val healthScoreDescription: String,
        val recommendations: List<Recommendation>
    ) : MealScanState()
    data class Unstructured(val text: String) : MealScanState()
    data class Error(val message: String) : MealScanState()
}

class MealScanViewModel : ViewModel() {
    private val _state = MutableStateFlow<MealScanState>(MealScanState.Loading)
    val state: StateFlow<MealScanState> = _state

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = MealScanState.Loading
            try {
                // compress & convert on IO
                val imageBase64 = withContext(Dispatchers.IO) {
                    ImageUtils.bitmapToBase64(bitmap)
                }

                val result = withContext(Dispatchers.IO) {
                    OpenAIVisionService.analyzeFoodImage(imageBase64)
                }

                if (result.isSuccess) {
                    val content = result.getOrNull() ?: ""
                    val trimmed = content.trim()
                    // If model returned non-JSON descriptive text, treat as Unstructured so UI can show it
                    if (!trimmed.startsWith("{")) {
                        _state.value = MealScanState.Unstructured(trimmed.take(200))
                    } else {
                        // Parse JSON (service should return JSON)
                        val uiState = parseContentToState(content)
                        if (uiState != null) {
                            _state.value = uiState
                        } else {
                            _state.value = MealScanState.Error("Analysis returned invalid JSON")
                        }
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Unknown error"
                    _state.value = MealScanState.Error(err)
                }
            } catch (e: Exception) {
                _state.value = MealScanState.Error(e.message ?: "Unexpected error")
            }
        }
    }

    private fun parseContentToState(content: String): MealScanState? {
        val trimmed = content.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val json = JSONObject(trimmed)
            val mealIdentification = json.optString("mealIdentification", "")

            val nutritionObj = json.optJSONObject("nutrition")
            val nutrition = nutritionObj?.let {
                NutritionDetails(
                    calories = it.optInt("calories", 0),
                    protein = it.optInt("protein", 0),
                    carbs = it.optInt("carbs", 0),
                    fat = it.optInt("fat", 0),
                    fiber = it.optInt("fiber", 0).takeIf { v -> v > 0 },
                    sugars = it.optInt("sugars", 0).takeIf { v -> v > 0 },
                    sodium = it.optInt("sodium", 0).takeIf { v -> v > 0 },
                    cholesterol = it.optInt("cholesterol", 0).takeIf { v -> v > 0 }
                )
            }

            val healthScore = json.optDouble("healthScore", 0.0)
            val healthScoreDesc = json.optString("healthScoreDescription", "")

            val recs = mutableListOf<Recommendation>()
            val recArr = json.optJSONArray("recommendations")
            if (recArr != null) {
                for (i in 0 until recArr.length()) {
                    val r = recArr.getJSONObject(i)
                    recs.add(Recommendation(title = r.optString("title", ""), description = r.optString("description", "")))
                }
            }

            MealScanState.Success(mealIdentification, nutrition, healthScore, healthScoreDesc, recs)
        } catch (e: Exception) {
            null
        }
    }
}
