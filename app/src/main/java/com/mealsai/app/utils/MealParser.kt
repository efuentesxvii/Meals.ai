package com.mealsai.app.utils

import com.mealsai.app.model.Ingredient
import com.mealsai.app.model.Meal
import org.json.JSONArray
import org.json.JSONObject

object MealParser {
    
    /**
     * Parse OpenAI response JSON string into list of Meal objects
     * Expected format: JSON array of meal objects
     */
    fun parseMealsFromJSON(jsonString: String): List<Meal> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val meals = mutableListOf<Meal>()
            
            for (i in 0 until jsonArray.length()) {
                val mealObj = jsonArray.getJSONObject(i)
                val meal = parseMeal(mealObj)
                meals.add(meal)
            }
            
            meals
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Parse OpenAI text response into Meal objects
     * Handles both JSON and text formats
     */
    fun parseMealsFromText(text: String): List<Meal> {
        return try {
            // Try to parse as JSON first
            if (text.trim().startsWith("[")) {
                parseMealsFromJSON(text)
            } else {
                // Parse text format - look for meal patterns
                parseMealsFromTextFormat(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseMeal(mealObj: JSONObject): Meal {
        val title = mealObj.optString("title", "Untitled Meal")
        val description = mealObj.optString("description", "")
        val time = mealObj.optString("time", "30 min")
        val calories = mealObj.optString("calories", "300 cal")
        val servings = mealObj.optString("servings", "2 servings")
        val difficulty = mealObj.optString("difficulty", "Easy")
        
        val tags = mutableListOf<String>()
        val tagsArray = mealObj.optJSONArray("tags")
        if (tagsArray != null) {
            for (j in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(j))
            }
        }
        
        val ingredients = mutableListOf<Ingredient>()
        val ingredientsArray = mealObj.optJSONArray("ingredients")
        if (ingredientsArray != null) {
            for (j in 0 until ingredientsArray.length()) {
                val ingObj = ingredientsArray.getJSONObject(j)
                ingredients.add(
                    Ingredient(
                        name = ingObj.optString("name", ""),
                        quantity = ingObj.optString("quantity", ""),
                        category = ingObj.optString("category", "Other")
                    )
                )
            }
        }
        
        return Meal(
            title = title,
            description = description,
            time = time,
            calories = calories,
            servings = servings,
            tags = tags,
            difficulty = difficulty,
            ingredients = ingredients
        )
    }
    
    private fun parseMealsFromTextFormat(text: String): List<Meal> {
        // Fallback: create a simple meal from text
        val lines = text.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        
        // Create a basic meal from the text
        val title = lines.firstOrNull()?.take(50) ?: "AI Generated Meal"
        val description = lines.drop(1).take(3).joinToString(" ")
        
        return listOf(
            Meal(
                title = title,
                description = description.ifEmpty { "A delicious AI-generated meal suggestion." },
                time = "30 min",
                calories = "300 cal",
                servings = "2 servings",
                tags = listOf("AI Generated"),
                difficulty = "Easy",
                ingredients = emptyList()
            )
        )
    }
    
    /**
     * Create a prompt for OpenAI to generate meals
     */
    fun createMealGenerationPrompt(preferences: String = ""): String {
        return """
            Generate 6 diverse, healthy meal suggestions in JSON format. 
            Each meal should include: title, description, time (e.g., "30 min"), 
            calories (e.g., "450 cal"), servings (e.g., "2 servings"), 
            tags (array of strings like ["Vegetarian", "High-Protein"]), 
            difficulty ("Easy", "Medium", or "Hard"), 
            and ingredients (array of objects with name, quantity, and category).
            
            Categories for ingredients: "Produce", "Meat", "Dairy & Eggs", "Other"
            
            ${if (preferences.isNotEmpty()) "User preferences: $preferences" else ""}
            
            Return ONLY a valid JSON array, no markdown, no code blocks, just the JSON array.
            Format: [{"title": "...", "description": "...", "time": "...", "calories": "...", 
            "servings": "...", "tags": [...], "difficulty": "...", "ingredients": [{"name": "...", "quantity": "...", "category": "..."}]}, ...]
        """.trimIndent()
    }
}
