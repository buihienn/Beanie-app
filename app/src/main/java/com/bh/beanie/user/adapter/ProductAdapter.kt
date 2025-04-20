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
import com.bh.beanie.model.Product
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bh.beanie.repository.FavoriteRepository
import com.bh.beanie.user.fragment.ProductDetailFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(private val context: Context, private val productList: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val favoriteRepository = FavoriteRepository(FirebaseFirestore.getInstance())
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val favorites = mutableMapOf<String, Boolean>()

    init {
        // Kiểm tra favorites cho mỗi sản phẩm nếu user đã đăng nhập
        userId?.let { uid ->
            productList.forEach { product ->
                favoriteRepository.isFavorite(uid, product.id) { isFavorite ->
                    favorites[product.id] = isFavorite
                    notifyItemChanged(productList.indexOf(product))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Load hình ảnh từ URL sử dụng Glide
        if (product.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(product.imageUrl)
                .into(holder.productImageView)
        } else {
            holder.productImageView.setImageResource(R.drawable.placeholder)
        }

        holder.productNameTextView.text = product.name
        holder.productPriceTextView.text = String.format(Locale.getDefault(), "%.0fđ", product.price)

        // Cập nhật trạng thái favorite
        val isFavorite = favorites[product.id] ?: false
        holder.favoriteButton.icon = ContextCompat.getDrawable(
            context,
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        )

        holder.favoriteButton.setOnClickListener {
            userId?.let { uid ->
                val currentFavorite = favorites[product.id] ?: false
                val newFavorite = !currentFavorite

                if (newFavorite) {
                    favoriteRepository.addFavorite(uid, product)
                } else {
                    favoriteRepository.removeFavorite(uid, product.id)
                }

                favorites[product.id] = newFavorite
                holder.favoriteButton.icon = ContextCompat.getDrawable(
                    context,
                    if (newFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
                )
            }
        }

        holder.itemView.setOnClickListener {
            val productDetailFragment = ProductDetailFragment.newInstance(product)
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            productDetailFragment.show(fragmentManager, "productDetail")
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