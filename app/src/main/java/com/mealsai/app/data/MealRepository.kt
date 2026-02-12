package com.mealsai.app.data

import com.mealsai.app.model.Meal
import com.mealsai.app.model.Ingredient
import com.mealsai.app.services.FirebaseAuthService
import com.mealsai.app.services.FirestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object MealRepository {
    // In-memory cache
    private val savedMealsCache = mutableListOf<Meal>()
    private val plannedMealsCache = mutableListOf<PlannedMeal>()
    
    // State flows for reactive updates
    val savedMealsFlow: StateFlow<List<Meal>> = MutableStateFlow(emptyList())
    val plannedMealsFlow: StateFlow<List<PlannedMeal>> = MutableStateFlow(emptyList())

    data class PlannedMeal(
        val id: String,
        val date: String,
        val meal: Meal,
        val type: String
    )
    
    private fun getUserId(): String? {
        return FirebaseAuthService.getCurrentUser()?.uid
    }

    suspend fun addSavedMeal(meal: Meal): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val result = FirestoreService.saveMeal(userId, meal)
            if (result.isSuccess) {
                if (!savedMealsCache.contains(meal)) {
                    savedMealsCache.add(meal)
                    (savedMealsFlow as MutableStateFlow).value = savedMealsCache.toList()
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedMeals(): List<Meal> {
        val userId = getUserId() ?: return emptyList()
        
        return try {
            val result = FirestoreService.getSavedMeals(userId)
            if (result.isSuccess) {
                savedMealsCache.clear()
                savedMealsCache.addAll(result.getOrNull() ?: emptyList())
                (savedMealsFlow as MutableStateFlow).value = savedMealsCache.toList()
            }
            savedMealsCache.toList()
        } catch (e: Exception) {
            savedMealsCache.toList()
        }
    }
    
    suspend fun removeSavedMeal(meal: Meal): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val result = FirestoreService.removeSavedMeal(userId, meal.title)
            if (result.isSuccess) {
                savedMealsCache.remove(meal)
                (savedMealsFlow as MutableStateFlow).value = savedMealsCache.toList()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPlannedMeal(date: String, meal: Meal, type: String): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val result = FirestoreService.addPlannedMeal(userId, date, meal, type)
            if (result.isSuccess) {
                // Refresh planned meals
                loadPlannedMeals()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlannedMeals(): List<PlannedMeal> {
        return plannedMealsCache.toList()
    }
    
    suspend fun getPlannedMealsForDate(date: String): List<PlannedMeal> {
        loadPlannedMeals()
        return plannedMealsCache.filter { it.date == date }
    }
    
    suspend fun removePlannedMeal(plannedMeal: PlannedMeal): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val result = FirestoreService.removePlannedMeal(userId, plannedMeal.id)
            if (result.isSuccess) {
                plannedMealsCache.remove(plannedMeal)
                (plannedMealsFlow as MutableStateFlow).value = plannedMealsCache.toList()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loadPlannedMeals() {
        val userId = getUserId() ?: return
        
        try {
            val result = FirestoreService.getPlannedMeals(userId)
            if (result.isSuccess) {
                plannedMealsCache.clear()
                val plannedMealsData = result.getOrNull() ?: emptyList()
                plannedMealsCache.addAll(
                    plannedMealsData.map { data ->
                        PlannedMeal(
                            id = data.id,
                            date = data.date,
                            meal = data.meal,
                            type = data.mealType
                        )
                    }
                )
                (plannedMealsFlow as MutableStateFlow).value = plannedMealsCache.toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getGroceryItems(): Map<String, List<Ingredient>> {
        val allIngredients = mutableListOf<Ingredient>()
        plannedMealsCache.forEach { pm ->
             pm.meal.ingredients.forEach { ing ->
                 // Add source meal title for display
                 allIngredients.add(ing.copy(sourceMealTitle = pm.meal.title))
             }
        }
        // Group by category
        return allIngredients.groupBy { it.category }
    }
}
