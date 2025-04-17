package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.CategoryItem
import com.bumptech.glide.Glide

class AdminCategoryItemAdapter(
    private val items: List<CategoryItem>,
    private val onEditClick: (CategoryItem) -> Unit,
    private val onDeleteClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<AdminCategoryItemAdapter.CategoryItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_child_admin, parent, false)
        return CategoryItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CategoryItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.textNameItemInCategory)
        private val itemStock: TextView = itemView.findViewById(R.id.textStock)
        private val editButton: ImageButton = itemView.findViewById(R.id.imgBtnEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.imgBtnDelete)
        private val image: ImageView = itemView.findViewById(R.id.imageViewProductAdmin)

        fun bind(item: CategoryItem) {
            itemName.text = item.name
            itemStock.text = "Stock: ${item.stockQuantity} | Price: $${item.price}"

            editButton.setOnClickListener { onEditClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }

            Glide.with(itemView.context)
                .load(item.imageUrl) // Assuming `imageUrl` is a property in `CategoryItem`
                .placeholder(R.drawable.placeholder) // Placeholder image
                .error(R.drawable.ic_launcher_foreground) // Error image
                .into(image)

            editButton.setOnClickListener { onEditClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
            // You can add more details here
            itemView.setOnClickListener {
                // Show item details if needed
            }
        }
    }
}