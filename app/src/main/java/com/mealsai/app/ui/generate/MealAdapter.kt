package com.mealsai.app.ui.generate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mealsai.app.R
import com.mealsai.app.model.Meal
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealAdapter(
    private val meals: List<Meal>,
    private val onMealClick: ((Meal) -> Unit)? = null
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMealTitle: TextView = itemView.findViewById(R.id.tvMealTitle)
        val tvMealDesc: TextView = itemView.findViewById(R.id.tvMealDesc)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        val cgTags: ChipGroup = itemView.findViewById(R.id.cgTags)
        val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        val btnPlan: View = itemView.findViewById(R.id.btnPlan)
        val btnDetails: View = itemView.findViewById(R.id.btnDetails)
        val btnSave: android.widget.ImageButton = itemView.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_card, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.tvMealTitle.text = meal.title
        holder.tvMealDesc.text = meal.description
        holder.tvTime.text = meal.time
        
        holder.cgTags.removeAllViews()
        val context = holder.itemView.context
        
        meal.tags.forEach { tag ->
             val chip = Chip(context)
             chip.text = tag
             chip.isCheckable = false
             chip.isClickable = false
             chip.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
             chip.textSize = 12f
             
             if (tag.contains("Vegetarian") || tag.contains("Vegan") || tag.contains("Gluten")) {
                 chip.setChipBackgroundColorResource(android.R.color.holo_green_light)
                 chip.setTextColor(context.resources.getColor(android.R.color.black, null))
                 chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFECFDF3.toInt()) 
                 chip.setTextColor(0xFF027A48.toInt()) 
             } else if (tag.contains("Mediterranean") || tag.contains("Spicy")) {
                 chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFFFF6ED.toInt()) 
                 chip.setTextColor(0xFFC4320A.toInt()) 
             } else {
                 chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFF2F4F7.toInt())
                 chip.setTextColor(0xFF344054.toInt())
             }
             
             chip.ensureAccessibleTouchTarget(20) 
             holder.cgTags.addView(chip)
        }
        
        holder.tvDifficulty.text = meal.difficulty
        holder.tvDifficulty.setTextColor(0xFF027A48.toInt())

        // Check if saved - async check
        CoroutineScope(Dispatchers.Main).launch {
            val savedMeals = withContext(Dispatchers.IO) {
                com.mealsai.app.data.MealRepository.getSavedMeals()
            }
            val isSaved = savedMeals.any { it.title == meal.title }
            
            if (isSaved) {
                holder.btnSave.setImageResource(R.drawable.ic_bookmark_filled)
                holder.btnSave.setColorFilter(0xFFE91E63.toInt()) 
            } else {
                holder.btnSave.setImageResource(R.drawable.ic_bookmark_border)
                holder.btnSave.setColorFilter(0xFF101828.toInt())
            }
        }

        // Handle Save Button
        holder.btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val savedMeals = withContext(Dispatchers.IO) {
                    com.mealsai.app.data.MealRepository.getSavedMeals()
                }
                
                val isSaved = savedMeals.any { it.title == meal.title }
                
                if (isSaved) {
                    // Remove from saved
                    val result = withContext(Dispatchers.IO) {
                        com.mealsai.app.data.MealRepository.removeSavedMeal(meal)
                    }
                    if (result.isSuccess) {
                        holder.btnSave.setImageResource(R.drawable.ic_bookmark_border)
                        holder.btnSave.setColorFilter(0xFF101828.toInt())
                        Toast.makeText(context, "Meal Removed!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Add to saved
                    val result = withContext(Dispatchers.IO) {
                        com.mealsai.app.data.MealRepository.addSavedMeal(meal)
                    }
                    if (result.isSuccess) {
                        holder.btnSave.setImageResource(R.drawable.ic_bookmark_filled) 
                        holder.btnSave.setColorFilter(0xFFE91E63.toInt())
                        Toast.makeText(context, "Meal Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save meal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Handle Plan Meal Button Click
        holder.btnPlan.setOnClickListener {
            showPlanMealDialog(context, meal)
        }
        
        // Handle Details Button Click - Navigate to MealDetailsFragment
        holder.btnDetails.setOnClickListener {
            navigateToMealDetails(context, meal)
        }
        
        // Also allow clicking on the card itself to view details
        holder.itemView.setOnClickListener {
            navigateToMealDetails(context, meal)
        }
    }

    private fun showMealDetailsDialog(context: android.content.Context, meal: Meal) {
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_meal_details)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setDimAmount(0.6f)
        
        // Bind Data
        dialog.findViewById<TextView>(R.id.tvDetailTitle).text = meal.title
        dialog.findViewById<TextView>(R.id.tvDetailTime).text = meal.time
        dialog.findViewById<TextView>(R.id.tvDetailServings).text = meal.servings.replace(" servings", "").replace(" serving", "") // Just the number logic
        dialog.findViewById<TextView>(R.id.tvDetailDifficulty).text = meal.difficulty
        dialog.findViewById<TextView>(R.id.tvDetailCalories).text = meal.calories

        // Bind Tags
        val cgTags = dialog.findViewById<ChipGroup>(R.id.cgDetailTags)
        cgTags.removeAllViews()
        meal.tags.forEach { tag ->
             val chip = Chip(context)
             chip.text = tag
             chip.isCheckable = false
             chip.isClickable = false
             chip.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
             chip.textSize = 12f
             
             if (tag.contains("Vegetarian") || tag.contains("Vegan") || tag.contains("Gluten")) {
                 chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFECFDF3.toInt()) 
                 chip.setTextColor(0xFF027A48.toInt()) 
             } else {
                 chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFFFF6ED.toInt()) 
                 chip.setTextColor(0xFFC4320A.toInt()) 
             }
             chip.ensureAccessibleTouchTarget(20) 
             cgTags.addView(chip)
        }

        // Close Action
        dialog.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPlanMealDialog(context: android.content.Context, meal: Meal) {
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_plan_meal)
        
        // Transparent background for CardView corners
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        // Layout params to match screen width with margin
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.5f)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        tvTitle.text = "Plan Meal: ${meal.title}"

        val etDate = dialog.findViewById<TextView>(R.id.etDate)
        // Date Picker Logic
        etDate.setOnClickListener {
             // Basic DatePickerDialog for simplicity and reliability without fragment manager needing casting if context is wrapper
            val calendar = java.util.Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                R.style.DatePickerTheme,
                { _, year, month, dayOfMonth ->
                    // Use yyyy-MM-dd format for consistency
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etDate.text = selectedDate
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Meal Type Selection Logic
        val buttons = listOf(
            dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnTypeBreakfast),
            dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnTypeLunch),
            dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnTypeDinner),
            dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnTypeSnack)
        )

        var selectedType = "Lunch" // Default

        fun updateSelection(selectedBtn: android.view.View, type: String) {
            selectedType = type
            buttons.forEach { btn ->
                if (btn == selectedBtn) {
                     btn.setBackgroundResource(R.drawable.bg_gradient_orange)
                     btn.setTextColor(android.graphics.Color.WHITE)
                } else {
                     btn.setBackgroundResource(R.drawable.bg_input_field)
                     btn.setTextColor(0xFF344054.toInt())
                }
            }
        }

        // Default: Lunch
        updateSelection(buttons[1], "Lunch")

        buttons[0].setOnClickListener { updateSelection(it, "Breakfast") }
        buttons[1].setOnClickListener { updateSelection(it, "Lunch") }
        buttons[2].setOnClickListener { updateSelection(it, "Dinner") }
        buttons[3].setOnClickListener { updateSelection(it, "Snack") }

        // Action Buttons
        dialog.findViewById<View>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btnDialogAdd).setOnClickListener {
             val date = etDate.text.toString()
             if (date.isEmpty() || date == "Select date") {
                 Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
             }
             
             CoroutineScope(Dispatchers.Main).launch {
                 val result = withContext(Dispatchers.IO) {
                     com.mealsai.app.data.MealRepository.addPlannedMeal(date, meal, selectedType)
                 }
                 
                 if (result.isSuccess) {
                     Toast.makeText(context, "Added to Plan!", Toast.LENGTH_SHORT).show()
                     dialog.dismiss()
                 } else {
                     Toast.makeText(context, "Failed to add meal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                 }
             }
        }

        dialog.show()
    }

    private fun navigateToMealDetails(context: android.content.Context, meal: Meal) {
        if (onMealClick != null) {
            onMealClick.invoke(meal)
        } else {
            showMealDetailsDialog(context, meal)
        }
    }

    override fun getItemCount() = meals.size
}
