package com.mealsai.app.ui.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mealsai.app.R
import com.mealsai.app.data.MealRepository.PlannedMeal

class PlannedMealAdapter(
    private val meals: List<PlannedMeal>,
    private val onDeleteClick: (PlannedMeal) -> Unit
) : RecyclerView.Adapter<PlannedMealAdapter.PlannedMealViewHolder>() {

    class PlannedMealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        val tvMealName: TextView = itemView.findViewById(R.id.tvMealName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannedMealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_planned_meal, parent, false)
        return PlannedMealViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlannedMealViewHolder, position: Int) {
        val plannedMeal = meals[position]
        holder.tvMealType.text = plannedMeal.type
        holder.tvMealName.text = plannedMeal.meal.title

        holder.btnDelete.setOnClickListener {
            onDeleteClick(plannedMeal)
        }
    }

    override fun getItemCount() = meals.size
}
