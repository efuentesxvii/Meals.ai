package com.mealsai.app.model

import java.io.Serializable

data class Meal(
    val title: String,
    val description: String,
    val time: String,
    val calories: String,
    val servings: String,
    val tags: List<String>,
    val difficulty: String,
    val ingredients: List<Ingredient> = emptyList(),
    val nutritionDetails: NutritionDetails? = null,
    val recommendations: List<Recommendation> = emptyList()
) : Serializable

data class Ingredient(
    val name: String,
    val quantity: String,
    val category: String,
    val sourceMealTitle: String = "" // To track which meal it came from
) : Serializable

data class NutritionDetails(
    val calories: Int,
    val protein: Int, // grams
    val carbs: Int, // grams
    val fat: Int, // grams
    val fiber: Int? = null, // grams
    val sugars: Int? = null, // grams
    val sodium: Int? = null, // mg
    val cholesterol: Int? = null // mg
) : Serializable

data class Recommendation(
    val title: String,
    val description: String
) : Serializable

