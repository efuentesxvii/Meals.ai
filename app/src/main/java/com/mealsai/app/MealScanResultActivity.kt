package com.mealsai.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mealsai.app.data.MealRepository
import com.mealsai.app.model.Meal
import com.mealsai.app.model.Ingredient
import com.mealsai.app.model.NutritionDetails
import com.mealsai.app.model.Recommendation

import com.mealsai.app.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.mealsai.app.ui.scan.MealScanViewModel

class MealScanResultActivity : AppCompatActivity() {
    companion object { private const val TAG = "MealScanResultActivity" }

    private var analyzedMeal: Meal? = null
    private var imageBitmap: Bitmap? = null

    private val viewModel: MealScanViewModel by lazy { ViewModelProvider(this).get(MealScanViewModel::class.java) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_scan_result)

        setupViews()

        // Observe ViewModel state
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                renderState(state)
            }
        }

        loadImage()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }
        
        // Save button - add meal to plans on current date
        findViewById<MaterialButton>(R.id.btnSaveMeal).setOnClickListener {
            saveMealToPlan()
        }
    }
    
    private fun loadImage() {
        val mealImage = findViewById<ImageView>(R.id.ivMealImage)

        Log.i(TAG, "loadImage started")

        // Try to get image from intent
        val imageUriString = intent.getStringExtra("imageUri")
        val imageBitmapBytes = intent.getByteArrayExtra("imageBitmap")

        when {
            imageUriString != null -> {
                // Load from URI (gallery)
                try {
                    val imageUri = Uri.parse(imageUriString)
                    mealImage.setImageURI(imageUri)
                    imageBitmap = ImageUtils.getBitmapFromUri(imageUri, this)
                    Log.i(TAG, "Loaded bitmap from URI: ${imageUri} (bitmap=${imageBitmap != null})")
                    imageBitmap?.let {
                        // Do NOT render any meal data yet — start ViewModel analysis
                        viewModel.analyzeImage(it)
                    } ?: run { Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image from URI", e)
                    e.printStackTrace()
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            imageBitmapBytes != null -> {
                // Load from bitmap bytes (camera)
                try {
                    val bitmap = BitmapFactory.decodeByteArray(imageBitmapBytes, 0, imageBitmapBytes.size)
                    mealImage.setImageBitmap(bitmap)
                    imageBitmap = bitmap
                    Log.i(TAG, "Loaded bitmap from bytes (size=${imageBitmapBytes.size})")
                    viewModel.analyzeImage(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image from bytes", e)
                    e.printStackTrace()
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // If no image provided, show a clear error and avoid stuck loading state
                Log.w(TAG, "No image provided to activity")
                showLoadingState(false)
                findViewById<TextView>(R.id.tvMealIdentification)?.text = "No image provided"
                findViewById<TextView>(R.id.tvNutritionalSummary)?.text = "Please select or capture an image to analyze."
                val root = findViewById<View>(android.R.id.content)
                com.google.android.material.snackbar.Snackbar.make(root, "No image provided to analyze", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showLoadingState(show: Boolean) {
        val saveBtn = findViewById<MaterialButton>(R.id.btnSaveMeal)
        val overlay = findViewById<View>(R.id.flLoadingOverlay)
        val progress = findViewById<ProgressBar>(R.id.progressOverlay)

        saveBtn.isEnabled = !show

        if (show) {
            // Bring overlay and spinner to front and make sure they block interaction
            overlay?.bringToFront()
            overlay?.elevation = 50f
            progress?.elevation = 60f
            overlay?.isClickable = true
            overlay?.isFocusable = true
            overlay?.visibility = View.VISIBLE

            findViewById<TextView>(R.id.tvMealIdentification).text = "Analyzing meal..."
            findViewById<TextView>(R.id.tvNutritionalSummary).text = ""
        } else {
            overlay?.isClickable = false
            overlay?.isFocusable = false
            overlay?.visibility = View.GONE
        }
    }
    

    
    private fun parseAndDisplayAnalysis(jsonString: String) {
        try {
            // If the model returned plain descriptive text (not JSON), handle it gracefully
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{")) {
                // Not JSON — show the descriptive text so the user sees what the model said
                val description = if (trimmed.length > 120) trimmed.substring(0, 120) + "..." else trimmed
                findViewById<TextView>(R.id.tvMealIdentification).text = description
                findViewById<TextView>(R.id.tvNutritionalSummary).text = trimmed

                // Clear/placeholder the nutrition UI (we don't fabricate numbers)
                findViewById<TextView>(R.id.tvCalories)?.text = "-"
                findViewById<TextView>(R.id.tvProtein)?.text = "-"
                findViewById<TextView>(R.id.tvCarbs)?.text = "-"
                findViewById<TextView>(R.id.tvFat)?.text = "-"
                findViewById<TextView>(R.id.tvHealthScore)?.text = "-" 
                findViewById<LinearProgressIndicator>(R.id.progressHealthScore)?.progress = 0

                showLoadingState(false)
                Toast.makeText(this, "Analysis returned an unstructured description; showing it directly.", Toast.LENGTH_LONG).show()
                return
            }

            val json = JSONObject(jsonString)
            
            val mealIdentification = json.optString("mealIdentification", "Food item")
            val nutritionObj = json.optJSONObject("nutrition")
            val healthScore = json.optDouble("healthScore", 8.0)
            val healthScoreDesc = json.optString("healthScoreDescription", "Good nutritional balance.")
            val recommendationsArray = json.optJSONArray("recommendations")
            val ingredientsArray = json.optJSONArray("ingredients")
            
            // Parse nutrition
            val nutrition = nutritionObj?.let {
                NutritionDetails(
                    calories = it.optInt("calories", 450),
                    protein = it.optInt("protein", 30),
                    carbs = it.optInt("carbs", 40),
                    fat = it.optInt("fat", 10),
                    fiber = it.optInt("fiber", 0).takeIf { it > 0 },
                    sugars = it.optInt("sugars", 0).takeIf { it > 0 },
                    sodium = it.optInt("sodium", 0).takeIf { it > 0 },
                    cholesterol = it.optInt("cholesterol", 0).takeIf { it > 0 }
                )
            }
            
            // Parse recommendations
            val recommendations = mutableListOf<Recommendation>()
            if (recommendationsArray != null) {
                for (i in 0 until recommendationsArray.length()) {
                    val recObj = recommendationsArray.getJSONObject(i)
                    recommendations.add(
                        Recommendation(
                            title = recObj.optString("title", ""),
                            description = recObj.optString("description", "")
                        )
                    )
                }
            }
            
            // Parse ingredients
            val ingredients = mutableListOf<Ingredient>()
            if (ingredientsArray != null) {
                for (i in 0 until ingredientsArray.length()) {
                    val ingObj = ingredientsArray.getJSONObject(i)
                    ingredients.add(
                        Ingredient(
                            name = ingObj.optString("name", ""),
                            quantity = ingObj.optString("quantity", ""),
                            category = ingObj.optString("category", "Other")
                        )
                    )
                }
            }
            
            // Create meal object
            analyzedMeal = Meal(
                title = mealIdentification,
                description = healthScoreDesc,
                time = "30 min", // Default, could be estimated
                calories = "${nutrition?.calories ?: 450} cal",
                servings = "1 serving",
                tags = listOf("Scanned", "AI Analyzed"),
                difficulty = "Easy",
                ingredients = ingredients,
                nutritionDetails = nutrition,
                recommendations = recommendations
            )
            
            // Update UI
            updateUI(mealIdentification, healthScoreDesc, nutrition, healthScore, recommendations)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing analysis: ${e.message}", Toast.LENGTH_SHORT).show()
            // Instead of fabricating data, show a fallback message so user knows parsing failed
            findViewById<TextView>(R.id.tvMealIdentification).text = "Analysis could not be parsed"
            findViewById<TextView>(R.id.tvNutritionalSummary).text = "The AI returned data we couldn't parse into structured nutrition info."
            showLoadingState(false)
        }
    }
    
    private fun updateUI(
        mealIdentification: String,
        summary: String,
        nutrition: NutritionDetails?,
        healthScore: Double,
        recommendations: List<Recommendation>
    ) {
        // Update meal identification
        findViewById<TextView>(R.id.tvMealIdentification).text = mealIdentification
        
        // Update nutritional summary
        findViewById<TextView>(R.id.tvNutritionalSummary).text = summary
        
        // Update nutrition metrics
        nutrition?.let {
            findViewById<TextView>(R.id.tvCalories)?.text = "${it.calories}"
            findViewById<TextView>(R.id.tvProtein)?.text = "${it.protein}g"
            findViewById<TextView>(R.id.tvCarbs)?.text = "${it.carbs}g"
            findViewById<TextView>(R.id.tvFat)?.text = "${it.fat}g"
        }
        
        // Update health score
        val progressHealthScore = findViewById<LinearProgressIndicator>(R.id.progressHealthScore)
        val healthScorePercent = (healthScore * 10).toInt() // Convert 0-10 to 0-100
        progressHealthScore?.progress = healthScorePercent
        findViewById<TextView>(R.id.tvHealthScore)?.text = "${String.format("%.1f", healthScore)}/10"
        findViewById<TextView>(R.id.tvHealthScoreDesc)?.text = summary
        
        // Update recommendations
        updateRecommendations(recommendations)
        
        showLoadingState(false)
    }
    
    private fun updateRecommendations(recommendations: List<Recommendation>) {
        val cvRecommendations = findViewById<CardView>(R.id.cvRecommendations)
        val rec1 = cvRecommendations.findViewById<CardView>(R.id.cvRec1)
        val rec2 = cvRecommendations.findViewById<CardView>(R.id.cvRec2)
        val rec3 = cvRecommendations.findViewById<CardView>(R.id.cvRec3)
        
        val recCards = listOfNotNull(rec1, rec2, rec3)
        
        // Update existing recommendation cards
        recommendations.take(recCards.size).forEachIndexed { index, rec ->
            val recCard = recCards[index]
            when (index) {
                0 -> {
                    recCard.findViewById<TextView>(R.id.tvRec1Title)?.text = rec.title
                    recCard.findViewById<TextView>(R.id.tvRec1Desc)?.text = rec.description
                }
                1 -> {
                    recCard.findViewById<TextView>(R.id.tvRec2Title)?.text = rec.title
                    recCard.findViewById<TextView>(R.id.tvRec2Desc)?.text = rec.description
                }
                2 -> {
                    recCard.findViewById<TextView>(R.id.tvRec3Title)?.text = rec.title
                    recCard.findViewById<TextView>(R.id.tvRec3Desc)?.text = rec.description
                }
            }
            recCard.visibility = View.VISIBLE
        }
        
        // Hide unused recommendation cards
        for (i in recommendations.size until recCards.size) {
            recCards[i].visibility = View.GONE
        }
    }
    
    private fun displayDefaultMeal() {
        // Fallback to default meal
        analyzedMeal = Meal(
            title = "Grilled Chicken Breast with Quinoa",
            description = "A well-balanced meal with high protein content from the chicken, complex carbohydrates from quinoa, and essential vitamins from the vegetables.",
            time = "30 min",
            calories = "450 cal",
            servings = "1 serving",
            tags = listOf("High-Protein", "Balanced", "Healthy"),
            difficulty = "Easy",
            ingredients = listOf(
                Ingredient("Chicken breast", "1 piece", "Meat"),
                Ingredient("Quinoa", "1/2 cup", "Other"),
                Ingredient("Mixed vegetables", "1 cup", "Produce"),
                Ingredient("Olive oil", "1 tbsp", "Other")
            )
        )
        
        updateUI(
            "Grilled chicken breast with quinoa and roasted vegetables",
            "This is a well-balanced meal with high protein content from the chicken, complex carbohydrates from quinoa, and essential vitamins from the vegetables.",
            NutritionDetails(450, 32, 48, 12, 8, 5, 420, 65),
            8.5,
            emptyList()
        )
    }
    
    private fun renderState(state: com.mealsai.app.ui.scan.MealScanState) {
        when (state) {
            is com.mealsai.app.ui.scan.MealScanState.Loading -> {
                // Show spinner and prevent actions
                showLoadingState(true)
                // Clear UI placeholders
                findViewById<TextView>(R.id.tvMealIdentification)?.text = "Analyzing meal..."
                findViewById<TextView>(R.id.tvNutritionalSummary)?.text = ""
                findViewById<TextView>(R.id.tvCalories)?.text = "-"
                findViewById<TextView>(R.id.tvProtein)?.text = "-"
                findViewById<TextView>(R.id.tvCarbs)?.text = "-"
                findViewById<TextView>(R.id.tvFat)?.text = "-"
                findViewById<TextView>(R.id.tvHealthScore)?.text = "-"
                findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressHealthScore)?.progress = 0
                updateRecommendations(emptyList())
            }
            is com.mealsai.app.ui.scan.MealScanState.Success -> {
                showLoadingState(false)
                updateUI(
                    state.mealIdentification,
                    state.healthScoreDescription,
                    state.nutrition,
                    state.healthScore,
                    state.recommendations
                )

                // Build a minimal Meal object so the Save button can use it
                analyzedMeal = com.mealsai.app.model.Meal(
                    title = state.mealIdentification,
                    description = state.healthScoreDescription,
                    time = "",
                    calories = state.nutrition?.calories?.let { "${it} cal" } ?: "-",
                    servings = "1 serving",
                    tags = listOf("Scanned"),
                    difficulty = "",
                    ingredients = emptyList(),
                    nutritionDetails = state.nutrition,
                    recommendations = state.recommendations
                )
            }
            is com.mealsai.app.ui.scan.MealScanState.Unstructured -> {
                showLoadingState(false)
                val text = state.text
                val short = if (text.length > 120) text.substring(0, 120) + "..." else text
                findViewById<TextView>(R.id.tvMealIdentification)?.text = short
                findViewById<TextView>(R.id.tvNutritionalSummary)?.text = text

                // Clear numerical UI
                findViewById<TextView>(R.id.tvCalories)?.text = "-"
                findViewById<TextView>(R.id.tvProtein)?.text = "-"
                findViewById<TextView>(R.id.tvCarbs)?.text = "-"
                findViewById<TextView>(R.id.tvFat)?.text = "-"
                findViewById<TextView>(R.id.tvHealthScore)?.text = "-"
                findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressHealthScore)?.progress = 0
                updateRecommendations(emptyList())

                // Show a gentle snackbar to the user that model returned a descriptive text
                val root = findViewById<View>(android.R.id.content)
                com.google.android.material.snackbar.Snackbar.make(root, "Analysis returned descriptive text.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Retry") { imageBitmap?.let { viewModel.analyzeImage(it) } }
                    .show()
            }
            is com.mealsai.app.ui.scan.MealScanState.Error -> {
                showLoadingState(false)
                findViewById<TextView>(R.id.tvMealIdentification)?.text = "Analysis failed"
                findViewById<TextView>(R.id.tvNutritionalSummary)?.text = state.message

                // Clear numerical UI
                findViewById<TextView>(R.id.tvCalories)?.text = "-"
                findViewById<TextView>(R.id.tvProtein)?.text = "-"
                findViewById<TextView>(R.id.tvCarbs)?.text = "-"
                findViewById<TextView>(R.id.tvFat)?.text = "-"
                findViewById<TextView>(R.id.tvHealthScore)?.text = "-"
                findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressHealthScore)?.progress = 0
                updateRecommendations(emptyList())

                val root = findViewById<View>(android.R.id.content)
                com.google.android.material.snackbar.Snackbar.make(root, "Analysis failed: ${state.message}", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Retry") { imageBitmap?.let { viewModel.analyzeImage(it) } }
                    .show()
            }
        }
    }

    private fun saveMealToPlan() {
        val meal = analyzedMeal ?: run {
            Toast.makeText(this, "No meal analyzed yet", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get current date in the format used by the app
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add to planned meals for today with "Lunch" as default meal type
                val result = withContext(Dispatchers.IO) {
                    MealRepository.addPlannedMeal(currentDate, meal, "Lunch")
                }
                
                if (result.isSuccess) {
                    Toast.makeText(this@MealScanResultActivity, "Meal saved to plan for today!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MealScanResultActivity, 
                        "Error saving meal: ${result.exceptionOrNull()?.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MealScanResultActivity, "Error saving meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
