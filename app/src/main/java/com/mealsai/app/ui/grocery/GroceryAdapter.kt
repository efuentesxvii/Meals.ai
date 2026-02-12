package com.mealsai.app.ui.grocery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mealsai.app.R
import com.mealsai.app.model.Ingredient

sealed class GroceryItem {
    data class Header(val category: String, val count: Int, val total: Int) : GroceryItem()
    data class Item(val ingredient: Ingredient, var isChecked: Boolean = false) : GroceryItem()
}

class GroceryAdapter(
    private var items: List<GroceryItem>,
    private val onItemChecked: (Int) -> Unit // Update progress
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    fun updateItems(newItems: List<GroceryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GroceryItem.Header -> TYPE_HEADER
            is GroceryItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grocery_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grocery_item, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val item = items[position] as GroceryItem.Header
                holder.tvCategoryName.text = item.category
                holder.tvItemCount.text = "${item.count}/${item.total}"
            }
            is ItemViewHolder -> {
                val item = items[position] as GroceryItem.Item
                holder.tvItemName.text = "${item.ingredient.quantity} ${item.ingredient.name}"
                holder.tvSourceMeal.text = "From: ${item.ingredient.sourceMealTitle}"
                holder.rbItem.isChecked = item.isChecked
                
                holder.itemView.setOnClickListener {
                    item.isChecked = !item.isChecked
                    holder.rbItem.isChecked = item.isChecked
                    onItemChecked(position)
                }
                
                holder.rbItem.setOnClickListener {
                    item.isChecked = !item.isChecked
                    onItemChecked(position)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvSourceMeal: TextView = itemView.findViewById(R.id.tvSourceMeal)
        val rbItem: RadioButton = itemView.findViewById(R.id.rbItem)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }
}
