package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem

class AdminCategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit,
    private val onEditItemClick: (CategoryItem) -> Unit,
    private val onDeleteItemClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<AdminCategoryAdapter.CategoryViewHolder>() {

    // Sử dụng Set để lưu các vị trí đang mở
    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_admin, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val isExpanded = expandedPositions.contains(position)

        holder.bind(category, isExpanded)

        // Handle click to expand/collapse and trigger onCategoryClick
        holder.itemView.setOnClickListener {
            if (isExpanded) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
            onCategoryClick(category) // Trigger the callback
        }
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.nameCategory)
        private val rvItems: RecyclerView = itemView.findViewById(R.id.recyclerItemCategory)

        fun bind(category: Category, isExpanded: Boolean) {
            categoryName.text = category.name

            // Thiết lập RecyclerView con
            rvItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = AdminCategoryItemAdapter(
                    items = category.items,
                    onEditClick = onEditItemClick,
                    onDeleteClick = onDeleteItemClick
                )
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
                visibility = if (isExpanded) View.VISIBLE else View.GONE
            }
        }
    }
}