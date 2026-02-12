package com.mealsai.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mealsai.app.data.MealRepository
import com.mealsai.app.ui.generate.MealAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedMealsFragment : Fragment() {

    private lateinit var rvSavedMeals: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_meals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvSavedMeals = view.findViewById(R.id.rvSavedMeals)
        rvSavedMeals.layoutManager = LinearLayoutManager(context)
        
        loadSavedMeals()
    }
    
    override fun onResume() {
        super.onResume()
        loadSavedMeals()
    }

    private fun loadSavedMeals() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val savedMeals = withContext(Dispatchers.IO) {
                    MealRepository.getSavedMeals()
                }
                
                // Update Count
                val tvCount = view?.findViewById<android.widget.TextView>(R.id.tvCount)
                tvCount?.text = "${savedMeals.size} saved"

                rvSavedMeals.adapter = MealAdapter(savedMeals) { meal ->
                    navigateToMealDetails(meal)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading saved meals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToMealDetails(meal: com.mealsai.app.model.Meal) {
        val fragment = com.mealsai.app.MealDetailsFragment.newInstance(meal)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("meal_details")
            .commit()
    }
}
