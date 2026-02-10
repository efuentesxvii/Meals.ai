package com.mealsai.app.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val dateOfBirth: String = "", // Format: "DD/MM/YYYY"
    val height: String = "",
    val weight: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "email" to email,
            "age" to age,
            "dateOfBirth" to dateOfBirth,
            "height" to height,
            "weight" to weight,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}
