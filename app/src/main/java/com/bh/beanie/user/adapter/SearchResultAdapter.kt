package com.bh.beanie.user.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Product
import com.bh.beanie.user.fragment.ProductDetailFragment
import com.bumptech.glide.Glide
import java.util.Locale

class SearchResultAdapter(
    private val context: Context,
    private val products: List<Product>,
    private val branchId: String,
    private val onCartUpdateListener: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<SearchResultAdapter.SearchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val product = products[position]

        // Hiển thị tên và giá sản phẩm
        holder.productNameTextView.text = product.name
        holder.productPriceTextView.text = String.format(Locale.getDefault(), "%.0fđ", product.price)

        // Tải hình ảnh sản phẩm
        if (product.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(product.imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(holder.productImageView)
        } else {
            holder.productImageView.setImageResource(R.drawable.placeholder)
        }

        // Xử lý sự kiện click vào item
        holder.itemView.setOnClickListener {
            val productDetailFragment = ProductDetailFragment.newInstance(
                branchId = branchId,
                categoryId = product.categoryId,
                productId = product.id
            )

            // Thiết lập listener để cập nhật giỏ hàng
            productDetailFragment.setProductDetailListener(object : ProductDetailFragment.ProductDetailListener {
                override fun onCartUpdated(itemCount: Int) {
                    onCartUpdateListener?.invoke(itemCount)
                }
            })

            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            productDetailFragment.show(fragmentManager, "productDetail")
        }
    }

    override fun getItemCount(): Int = products.size

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
    }
}