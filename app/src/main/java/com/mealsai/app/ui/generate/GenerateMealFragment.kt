package com.mealsai.app.ui.generate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.mealsai.app.CameraActivity
import com.mealsai.app.R
import com.mealsai.app.services.FirebaseAuthService
import com.mealsai.app.services.OpenAIService
import com.mealsai.app.utils.MealParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GenerateMealFragment : Fragment() {
    
    private lateinit var btnGenerate: MaterialButton
    private lateinit var llResults: View
    private lateinit var llSplash: View
    private lateinit var rvMeals: androidx.recyclerview.widget.RecyclerView
    private lateinit var btnCamera: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generate_meal, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        btnGenerate = view.findViewById(R.id.btnGenerate)
        llResults = view.findViewById(R.id.llResults)
        llSplash = view.findViewById(R.id.llSplash)
        rvMeals = view.findViewById(R.id.rvMeals)
        
        // Setup camera button and logout button from header
        val headerView = view.findViewById<View>(R.id.appHeader)
        btnCamera = headerView.findViewById(R.id.btnCamera)
        btnCamera.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }
        
        // Setup logout button
        val btnLogout = headerView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            (activity as? com.mealsai.app.MainActivity)?.handleLogout()
        }

        // Initial State
        llResults.visibility = View.GONE
        llSplash.visibility = View.VISIBLE
        
        btnGenerate.setOnClickListener {
            generateMealsWithAI()
        }
    }
    
    private fun generateMealsWithAI() {
        val currentUser = FirebaseAuthService.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to generate meals", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnGenerate.isEnabled = false
        llSplash.visibility = View.GONE
        llResults.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Create prompt for OpenAI
                val prompt = MealParser.createMealGenerationPrompt()
                
                // Call OpenAI API
                val result = withContext(Dispatchers.IO) {
                    OpenAIService.generateMeal(prompt)
                }
                
                if (result.isSuccess) {
                    val response = result.getOrNull() ?: ""
                    
                    // Parse the response
                    val meals = MealParser.parseMealsFromText(response)
                    
                    // If parsing failed or returned empty, use fallback meals
                    val finalMeals = if (meals.isEmpty()) {
                        getFallbackMeals()
                    } else {
                        meals.take(6) // Limit to 6 meals
                    }
                    
                    withContext(Dispatchers.Main) {
                        rvMeals.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                        rvMeals.adapter = MealAdapter(finalMeals) { meal ->
                            navigateToMealDetails(meal)
                        }
                        Toast.makeText(requireContext(), "Meals Generated!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // On error, use fallback meals
                    val error = result.exceptionOrNull()
                    withContext(Dispatchers.Main) {
                        val fallbackMeals = getFallbackMeals()
                        rvMeals.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                        rvMeals.adapter = MealAdapter(fallbackMeals)
                        Toast.makeText(requireContext(), 
                            "Using sample meals. ${error?.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                btnGenerate.isEnabled = true
            }
        }
    }
    
    private fun getFallbackMeals(): List<com.mealsai.app.model.Meal> {
        return listOf(
            com.mealsai.app.model.Meal(
                "Mediterranean Quinoa Bowl",
                "A nutritious bowl packed with quinoa, roasted vegetables, feta cheese, and lemon.",
                "40 min", "420 cal", "4 servings",
                listOf("Vegetarian", "Gluten-Free"),
                "Easy",
                listOf(
                    com.mealsai.app.model.Ingredient("Quinoa", "1 cup", "Other"),
                    com.mealsai.app.model.Ingredient("Vegetable broth", "2 cups", "Produce"),
                    com.mealsai.app.model.Ingredient("Red bell pepper, diced", "1", "Produce"),
                    com.mealsai.app.model.Ingredient("Cherry tomatoes", "1 cup", "Produce"),
                    com.mealsai.app.model.Ingredient("Feta cheese", "1/2 cup", "Dairy & Eggs")
                )
            ),
            com.mealsai.app.model.Meal(
                "Spicy Thai Red Curry",
                "Authentic Thai curry with coconut milk, bamboo shoots, and fresh basil.",
                "30 min", "550 cal", "2 servings",
                listOf("Vegan", "Dairy-Free", "Spicy"),
                "Medium",
                listOf(
                    com.mealsai.app.model.Ingredient("Coconut milk", "400ml", "Other"),
                    com.mealsai.app.model.Ingredient("Red curry paste", "2 tbsp", "Other"),
                    com.mealsai.app.model.Ingredient("Bamboo shoots", "1 can", "Produce"),
                    com.mealsai.app.model.Ingredient("Thai basil", "1 bunch", "Produce")
                )
            ),
            com.mealsai.app.model.Meal(
                "Grilled Lemon Herb Chicken",
                "Juicy grilled chicken breast marinated in fresh lemon juice and herbs.",
                "25 min", "320 cal", "2 servings",
                listOf("High-Protein", "Keto"),
                "Easy",
                listOf(
                    com.mealsai.app.model.Ingredient("Chicken breast", "2", "Meat"),
                    com.mealsai.app.model.Ingredient("Lemon", "1", "Produce"),
                    com.mealsai.app.model.Ingredient("Olive oil", "2 tbsp", "Other"),
                    com.mealsai.app.model.Ingredient("Mixed herbs", "1 tbsp", "Other")
                )
            ),
            com.mealsai.app.model.Meal(
                "Zucchini Noodle Pasta",
                "Fresh zucchini noodles tossed with homemade marinara and parmesan.",
                "20 min", "180 cal", "2 servings",
                listOf("Keto", "Vegetarian", "Low-Carb"),
                "Easy",
                listOf(
                    com.mealsai.app.model.Ingredient("Zucchini", "3", "Produce"),
                    com.mealsai.app.model.Ingredient("Marinara sauce", "1 cup", "Other"),
                    com.mealsai.app.model.Ingredient("Parmesan cheese", "1/4 cup", "Dairy & Eggs")
                )
            ),
            com.mealsai.app.model.Meal(
                "Baked Salmon with Asparagus",
                "Oven-baked salmon fillet served with garlic roasted asparagus.",
                "35 min", "450 cal", "1 serving",
                listOf("Paleo", "Gluten-Free"),
                "Medium",
                listOf(
                    com.mealsai.app.model.Ingredient("Salmon fillet", "1", "Meat"),
                    com.mealsai.app.model.Ingredient("Asparagus", "1 bunch", "Produce"),
                    com.mealsai.app.model.Ingredient("Lemon", "1", "Produce"),
                    com.mealsai.app.model.Ingredient("Garlic", "2 cloves", "Produce")
                )
            ),
            com.mealsai.app.model.Meal(
                "Mushroom Risotto",
                "Creamy arborio rice cooked with wild mushrooms and parmesan.",
                "45 min", "500 cal", "4 servings",
                listOf("Vegetarian", "Comfort Food"),
                "Hard",
                listOf(
                    com.mealsai.app.model.Ingredient("Arborio rice", "1.5 cups", "Other"),
                    com.mealsai.app.model.Ingredient("Mushrooms", "200g", "Produce"),
                    com.mealsai.app.model.Ingredient("Vegetable stock", "4 cups", "Other"),
                    com.mealsai.app.model.Ingredient("Parmesan", "1/2 cup", "Dairy & Eggs"),
                    com.mealsai.app.model.Ingredient("White wine", "1/2 cup", "Other")
                )
            )
        )
    }
    
    private fun navigateToMealDetails(meal: com.mealsai.app.model.Meal) {
        val fragment = com.mealsai.app.MealDetailsFragment.newInstance(meal)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("meal_details")
            .commit()
    }
    }

