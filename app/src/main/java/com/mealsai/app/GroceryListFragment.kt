package com.mealsai.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mealsai.app.data.MealRepository
import com.mealsai.app.model.Ingredient
import com.mealsai.app.ui.grocery.GroceryAdapter
import com.mealsai.app.ui.grocery.GroceryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroceryListFragment : Fragment() {
    
    private lateinit var rvGrocery: androidx.recyclerview.widget.RecyclerView
    private lateinit var cvContent: android.view.View
    private lateinit var clEmpty: android.view.View
    private lateinit var adapter: GroceryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grocery_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvGrocery = view.findViewById(R.id.rvGrocery)
        cvContent = view.findViewById(R.id.cvContent)
        clEmpty = view.findViewById(R.id.clEmpty) // This might change
        
        // Setup Adapter
        adapter = GroceryAdapter(emptyList()) { position: Int ->
            // Handle item checked
        }
        rvGrocery.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        rvGrocery.adapter = adapter
    }
    
    override fun onResume() {
        super.onResume()
        loadGroceries()
    }
    
    private fun loadGroceries() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load planned meals first to get grocery items
                withContext(Dispatchers.IO) {
                    MealRepository.loadPlannedMeals()
                }
                
                val groceryMap: Map<String, List<Ingredient>> = MealRepository.getGroceryItems()
                
                if (groceryMap.isEmpty()) {
                    rvGrocery.visibility = View.GONE
                    view?.findViewById<View>(R.id.clEmpty)?.visibility = View.VISIBLE
                } else {
                    rvGrocery.visibility = View.VISIBLE
                    view?.findViewById<View>(R.id.clEmpty)?.visibility = View.GONE
                    
                    // Flatten map to list
                    val displayList = mutableListOf<GroceryItem>()
                    
                    // Define specific order if needed, otherwise just iterate
                    val categoriesInOrder = listOf("Produce", "Meat", "Dairy & Eggs", "Other") // Example priority
                    val sortedKeys = groceryMap.keys.sortedBy { 
                        val index = categoriesInOrder.indexOf(it) 
                        if (index == -1) 999 else index
                    }
                    
                    sortedKeys.forEach { category ->
                        val ingredients: List<Ingredient> = groceryMap[category] ?: emptyList()
                        if (ingredients.isNotEmpty()) {
                            displayList.add(GroceryItem.Header(category, 0, ingredients.size))
                            ingredients.forEach { ingredient: Ingredient ->
                                displayList.add(GroceryItem.Item(ingredient))
                            }
                        }
                    }
                    
                    adapter.updateItems(displayList)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading groceries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

