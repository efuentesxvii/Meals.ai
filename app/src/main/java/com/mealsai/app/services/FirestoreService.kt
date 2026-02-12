package com.mealsai.app.services

import com.google.firebase.firestore.FirebaseFirestore
import com.mealsai.app.model.Meal
import com.mealsai.app.model.User
import kotlinx.coroutines.tasks.await

object FirestoreService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val USERS_COLLECTION = "users"
    private const val SAVED_MEALS_COLLECTION = "savedMeals"
    private const val PLANNED_MEALS_COLLECTION = "plannedMeals"
    
    suspend fun createUser(userId: String, user: User): Result<Unit> {
        return try {
            db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user?.copy(id = document.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, user: User): Result<Unit> {
        return try {
            val updateMap = user.toMap().toMutableMap()
            updateMap.remove("id")
            updateMap["updatedAt"] = System.currentTimeMillis()
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updateMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Saved Meals Operations
    suspend fun saveMeal(userId: String, meal: Meal): Result<Unit> {
        return try {
            val mealMap = mealToMap(meal)
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SAVED_MEALS_COLLECTION)
                .document(meal.title.hashCode().toString())
                .set(mealMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSavedMeals(userId: String): Result<List<Meal>> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SAVED_MEALS_COLLECTION)
                .get()
                .await()
            
            val meals = snapshot.documents.mapNotNull { doc ->
                val data = doc.data as? Map<*, *>
                data?.let { mapToMeal(it) }
            }
            Result.success(meals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeSavedMeal(userId: String, mealTitle: String): Result<Unit> {
        return try {
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SAVED_MEALS_COLLECTION)
                .document(mealTitle.hashCode().toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Planned Meals Operations
    suspend fun addPlannedMeal(userId: String, date: String, meal: Meal, mealType: String): Result<Unit> {
        return try {
            val plannedMealMap = mapOf(
                "date" to date,
                "mealType" to mealType,
                "meal" to mealToMap(meal),
                "createdAt" to System.currentTimeMillis()
            )
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PLANNED_MEALS_COLLECTION)
                .add(plannedMealMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlannedMeals(userId: String, date: String? = null): Result<List<PlannedMealData>> {
        return try {
            val collectionRef = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PLANNED_MEALS_COLLECTION)
            
            val snapshot = if (date != null) {
                collectionRef.whereEqualTo("date", date).get().await()
            } else {
                collectionRef.get().await()
            }
            
            val plannedMeals = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val mealData = data?.get("meal") as? Map<*, *>
                val meal = mealData?.let { mapToMeal(it) }
                
                if (meal != null) {
                    PlannedMealData(
                        id = doc.id,
                        date = data["date"] as? String ?: "",
                        mealType = data["mealType"] as? String ?: "Lunch",
                        meal = meal
                    )
                } else null
            }
            
            Result.success(plannedMeals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removePlannedMeal(userId: String, plannedMealId: String): Result<Unit> {
        return try {
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PLANNED_MEALS_COLLECTION)
                .document(plannedMealId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper functions
    private fun mealToMap(meal: Meal): Map<String, Any> {
        return mapOf(
            "title" to meal.title,
            "description" to meal.description,
            "time" to meal.time,
            "calories" to meal.calories,
            "servings" to meal.servings,
            "tags" to meal.tags,
            "difficulty" to meal.difficulty,
            "ingredients" to meal.ingredients.map { ing ->
                mapOf(
                    "name" to ing.name,
                    "quantity" to ing.quantity,
                    "category" to ing.category
                )
            }
        )
    }
    
    private fun mapToMeal(data: Map<*, *>): Meal? {
        return try {
            Meal(
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                time = data["time"] as? String ?: "30 min",
                calories = data["calories"] as? String ?: "300 cal",
                servings = data["servings"] as? String ?: "2 servings",
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                difficulty = data["difficulty"] as? String ?: "Easy",
                ingredients = (data["ingredients"] as? List<*>)?.mapNotNull { ingMap ->
                    val ing = ingMap as? Map<*, *>
                    if (ing != null) {
                        com.mealsai.app.model.Ingredient(
                            name = ing["name"] as? String ?: "",
                            quantity = ing["quantity"] as? String ?: "",
                            category = ing["category"] as? String ?: "Other"
                        )
                    } else null
                } ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    data class PlannedMealData(
        val id: String,
        val date: String,
        val mealType: String,
        val meal: Meal
    )
}
