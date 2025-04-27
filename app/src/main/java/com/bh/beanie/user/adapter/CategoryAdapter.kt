package com.bh.beanie.user.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Category
import com.bh.beanie.repository.ProductRepository
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val branchId: String,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val productRepository = ProductRepository(FirebaseFirestore.getInstance())
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryNameTextView.text = category.name

        // Lấy ảnh đại diện từ một sản phẩm trong danh mục
        loadCategoryImage(category.id, holder.categoryImageView)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    private fun loadCategoryImage(categoryId: String, imageView: ImageView) {
        coroutineScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productRepository.fetchProductsForCategory(branchId, categoryId, limit = 1)
                }

                if (products.isNotEmpty() && products[0].imageUrl.isNotEmpty()) {
                    Glide.with(context)
                        .load(products[0].imageUrl)
                        .placeholder(R.drawable.ic_category)
                        .error(R.drawable.ic_category)
                        .centerCrop()
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.ic_category)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.ic_category)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryImageView: ImageView = itemView.findViewById(R.id.categoryImageView)
        val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
    }
}