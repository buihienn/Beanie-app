package com.bh.beanie.user.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.user.model.Product
import androidx.core.content.ContextCompat

class ProductAdapter(private val context: Context, private val productList: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.productImageView.setImageResource(product.imageResourceId)
        holder.productNameTextView.text = product.name
        holder.productPriceTextView.text = String.format(Locale.getDefault(), "%.0fđ", product.price)

        holder.favoriteButton.icon = ContextCompat.getDrawable(
            context,
            if (product.isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        )

        holder.favoriteButton.setOnClickListener {
            product.isFavorite = !product.isFavorite
            holder.favoriteButton.icon = ContextCompat.getDrawable(
                context,
                if (product.isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
            )
        }
    }

    override fun getItemCount(): Int = productList.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        val favoriteButton: MaterialButton = itemView.findViewById(R.id.favoriteButton)
    }
}