package com.mealsai.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mealsai.app.data.MealRepository
import com.mealsai.app.model.Meal
import com.mealsai.app.model.Recommendation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealDetailsFragment : Fragment() {
    
    private var meal: Meal? = null
    
    companion object {
        private const val ARG_MEAL = "meal"
        
        fun newInstance(meal: Meal): MealDetailsFragment {
            val fragment = MealDetailsFragment()
            val args = Bundle()
            args.putSerializable(ARG_MEAL, meal)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meal_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get meal from arguments
        meal = arguments?.getSerializable(ARG_MEAL) as? Meal
        
        if (meal == null) {
            Toast.makeText(requireContext(), "Meal data not found", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }
        
        setupToolbar(view)
        displayMealDetails(view)
    }
    
    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }
    
    private fun displayMealDetails(view: View) {
        val meal = this.meal ?: return
        
        // Title
        view.findViewById<TextView>(R.id.tvTitle).text = meal.title
        
        // Quick info (Time, Servings, Difficulty)
        val llQuickInfo = view.findViewById<LinearLayout>(R.id.llQuickInfo)
        if (llQuickInfo.childCount >= 3) {
            val timeLayout = llQuickInfo.getChildAt(0) as? LinearLayout
            val servingsLayout = llQuickInfo.getChildAt(1) as? LinearLayout
            val difficultyLayout = llQuickInfo.getChildAt(2) as? LinearLayout
            
            timeLayout?.getChildAt(1)?.let { (it as? TextView)?.text = meal.time }
            servingsLayout?.getChildAt(1)?.let { (it as? TextView)?.text = meal.servings.replace(" servings", "").replace(" serving", "") }
            difficultyLayout?.getChildAt(1)?.let { (it as? TextView)?.text = meal.difficulty }
        }
        
        // Tags
        val cgTags = view.findViewById<ChipGroup>(R.id.cgTags)
        cgTags.removeAllViews()
        meal.tags.forEach { tag ->
            val chip = Chip(requireContext())
            chip.text = tag
            chip.isCheckable = false
            chip.isClickable = false
            
            when {
                tag.contains("Vegetarian") || tag.contains("Vegan") || tag.contains("Gluten") -> {
                    chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFECFDF3.toInt())
                    chip.setTextColor(0xFF027A48.toInt())
                }
                tag.contains("Mediterranean") || tag.contains("Spicy") -> {
                    chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFFFF6ED.toInt())
                    chip.setTextColor(0xFFC4320A.toInt())
                }
                else -> {
                    chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFF2F4F7.toInt())
                    chip.setTextColor(0xFF344054.toInt())
                }
            }
            cgTags.addView(chip)
        }
        
        // Nutrition Facts
        meal.nutritionDetails?.let { nutrition ->
            // Calories
            view.findViewById<TextView>(R.id.tvDetailCalories)?.text = "${nutrition.calories} kcal"

            // Protein, Carbs, Fat - these are in GridLayout
            val glMacros = view.findViewById<android.widget.GridLayout>(R.id.glMacros)
            if (glMacros != null && glMacros.childCount >= 4) {
                // Protein (second child)
                glMacros.getChildAt(1)?.findViewById<TextView>(R.id.tvDetailCalories)?.text = "${nutrition.protein}g"
                // Carbs (third child) 
                glMacros.getChildAt(2)?.findViewById<TextView>(R.id.tvDetailCalories)?.text = "${nutrition.carbs}g"
                // Fat (fourth child)
                glMacros.getChildAt(3)?.findViewById<TextView>(R.id.tvDetailCalories)?.text = "${nutrition.fat}g"
            }

            // Detailed nutrition table
            val llNutDetails = view.findViewById<LinearLayout>(R.id.llNutDetails)
            if (llNutDetails != null) {
                updateNutritionTable(llNutDetails, nutrition)
            }
        } ?: run {
            // Use estimated values from calories string
            val caloriesValue = meal.calories.replace(" cal", "").toIntOrNull() ?: 300
            view.findViewById<TextView>(R.id.tvDetailCalories)?.text = "$caloriesValue kcal"
        }
        
        // Recommendations
        if (meal.recommendations.isNotEmpty()) {
            displayRecommendations(view, meal.recommendations)
        }
    }
    
    private fun updateNutritionTable(llNutDetails: LinearLayout, nutrition: com.mealsai.app.model.NutritionDetails) {
        var index = 0
        nutrition.fiber?.let { fiber ->
            if (llNutDetails.childCount > index) {
                val row = llNutDetails.getChildAt(index) as? LinearLayout
                (row?.getChildAt(1) as? TextView)?.text = "${fiber}g"
            }
            index++
        }
        nutrition.sugars?.let { sugars ->
            if (llNutDetails.childCount > index) {
                val row = llNutDetails.getChildAt(index) as? LinearLayout
                (row?.getChildAt(1) as? TextView)?.text = "${sugars}g"
            }
            index++
        }
        nutrition.sodium?.let { sodium ->
            if (llNutDetails.childCount > index) {
                val row = llNutDetails.getChildAt(index) as? LinearLayout
                (row?.getChildAt(1) as? TextView)?.text = "${sodium}mg"
            }
            index++
        }
        nutrition.cholesterol?.let { chol ->
            if (llNutDetails.childCount > index) {
                val row = llNutDetails.getChildAt(index) as? LinearLayout
                (row?.getChildAt(1) as? TextView)?.text = "${chol}mg"
            }
        }
    }
    
    private fun displayRecommendations(view: View, recommendations: List<Recommendation>) {
        val lblRecs = view.findViewById<TextView>(R.id.lblRecs)
        val llRecs = view.findViewById<LinearLayout>(R.id.llRecs)
        
        if (lblRecs != null && llRecs != null) {
            lblRecs.visibility = View.VISIBLE
            llRecs.removeAllViews()
            
            recommendations.forEach { rec ->
                val recLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = context.getDrawable(R.drawable.bg_rounded_orange_light)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }
                }
                
                val titleView = TextView(requireContext()).apply {
                    text = rec.title
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF101828.toInt())
                }
                
                val descView = TextView(requireContext()).apply {
                    text = rec.description
                    textSize = 12f
                    setTextColor(0xFF555555.toInt())
                    setPadding(0, 4, 0, 0)
                }
                
                recLayout.addView(titleView)
                recLayout.addView(descView)
                llRecs.addView(recLayout)
            }
        }
    }
}

