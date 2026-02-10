package com.mealsai.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*

class WeeklyPlanFragment : Fragment() {

    private lateinit var rvDays: RecyclerView
    private lateinit var rvPlannedMeals: RecyclerView
    private lateinit var cvEmptyState: View
    private lateinit var tvDateRange: TextView
    private lateinit var dayAdapter: DayAdapter
    private val calendar = Calendar.getInstance()
    private val daysList = mutableListOf<DayModel>()
    private var selectedDayIndex = 0 // Default Today

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weekly_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvDays = view.findViewById(R.id.rvDays)
        rvPlannedMeals = view.findViewById(R.id.rvPlannedMeals)
        cvEmptyState = view.findViewById(R.id.cvEmptyState)
        tvDateRange = view.findViewById(R.id.tvDateRange)

        setupDaySelector(view)
        setupMealsList()
        updateDateRange()
    }

    override fun onResume() {
        super.onResume()
        loadMealsForSelectedDay() // Refresh when coming back
    }

    private fun setupDaySelector(view: View) {
        // Generate current week days
        val tempCal = Calendar.getInstance()
        // tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Start from Sunday? Or Just 7 days from today? 
        // Image shows "Jan 4 - Jan 10". Sun 4 ... Sat 10. So it's a fixed week view.
        // For demo, let's just do 7 days starting from today or fixed dummy if easiest.
        // Let's do dynamic 7 days starting today.
        
        daysList.clear()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())
        
        for (i in 0 until 7) {
            daysList.add(DayModel(
                dateFormat.format(tempCal.time),
                dayFormat.format(tempCal.time),
                getFullDateString(tempCal) // Store full date for query
            ))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        dayAdapter = DayAdapter(daysList) { index ->
            selectedDayIndex = index
            dayAdapter.notifyDataSetChanged()
            loadMealsForSelectedDay()
        }
        rvDays.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvDays.adapter = dayAdapter
    }
    
    private fun getFullDateString(cal: Calendar): String {
         // Use yyyy-MM-dd format for consistency
         val year = cal.get(Calendar.YEAR)
         val month = cal.get(Calendar.MONTH) + 1
         val day = cal.get(Calendar.DAY_OF_MONTH)
         return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun updateDateRange() {
        // Just showing range of daysList
        if (daysList.isNotEmpty()) {
             // Simple logic
             val start = daysList.first()
             val end = daysList.last()
             // Reconstruct fancy string or just "Jan 4 - Jan 10"
             // Simplified for demo
             tvDateRange.text = "Weekly Plan" 
        }
    }

    private fun setupMealsList() {
        rvPlannedMeals.layoutManager = LinearLayoutManager(context)
    }

    private fun loadMealsForSelectedDay() {
        val selectedDay = daysList[selectedDayIndex]
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load all planned meals first
                withContext(Dispatchers.IO) {
                    MealRepository.loadPlannedMeals()
                }
                
                val meals = withContext(Dispatchers.IO) {
                    MealRepository.getPlannedMealsForDate(selectedDay.fullDate)
                }
                
                if (meals.isEmpty()) {
                    cvEmptyState.visibility = View.VISIBLE
                    rvPlannedMeals.visibility = View.GONE
                } else {
                    cvEmptyState.visibility = View.GONE
                    rvPlannedMeals.visibility = View.VISIBLE
                    rvPlannedMeals.adapter = com.mealsai.app.ui.plan.PlannedMealAdapter(meals) { plannedMeal ->
                        // Handle Delete
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = withContext(Dispatchers.IO) {
                                MealRepository.removePlannedMeal(plannedMeal)
                            }
                            if (result.isSuccess) {
                                loadMealsForSelectedDay() // Refresh
                                Toast.makeText(context, "Meal removed from plan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to remove meal", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading meals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class DayModel(val name: String, val number: String, val fullDate: String)

    inner class DayAdapter(private val days: List<DayModel>, private val onClick: (Int) -> Unit) :
        RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
            val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
            val container: LinearLayout = itemView.findViewById(R.id.llDayContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_selector, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val day = days[position]
            holder.tvDayName.text = day.name
            holder.tvDayNumber.text = day.number

            if (position == selectedDayIndex) {
                holder.container.setBackgroundResource(R.drawable.bg_day_selector_selected)
                holder.tvDayNumber.setTextColor(0xFFFF5722.toInt()) // Orange
                holder.tvDayName.setTextColor(0xFFFF5722.toInt())
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_day_selector_unselected)
                holder.tvDayNumber.setTextColor(0xFF333333.toInt())
                holder.tvDayName.setTextColor(0xFF757575.toInt())
            }

            holder.itemView.setOnClickListener { onClick(position) }
        }

        override fun getItemCount() = days.size
    }
}
