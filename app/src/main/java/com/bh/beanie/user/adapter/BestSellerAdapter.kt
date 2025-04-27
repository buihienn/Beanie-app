package com.bh.beanie.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.databinding.ProductItemCardBinding
import com.bh.beanie.model.Product
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class BestSellerAdapter(
    private val products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<BestSellerAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ProductItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productNameTextView.text = product.name

            // Format giá tiền theo định dạng Việt Nam
            val formattedPrice = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                .format(product.price) + "đ"
            binding.productPriceTextView.text = formattedPrice

            // Load hình ảnh sản phẩm bằng Glide
            Glide.with(binding.root.context)
                .load(product.imageUrl)
                .centerCrop()
                .into(binding.productImageView)

            // Ẩn nút favorite
            binding.favoriteButton.visibility = android.view.View.GONE

            // Xử lý sự kiện click
            binding.root.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}