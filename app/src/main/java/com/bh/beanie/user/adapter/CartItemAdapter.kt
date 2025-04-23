package com.bh.beanie.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.OrderItem
import java.text.NumberFormat
import java.util.Locale

class CartItemAdapter(
    private val cartItems: List<OrderItem>,
    private val onEditItemClicked: (OrderItem, Int) -> Unit
) : RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cart_item_layout, parent, false)
        return CartItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val item = cartItems[position]
        holder.bind(item, position)
    }

    override fun getItemCount() = cartItems.size

    inner class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val sizeTextView: TextView = itemView.findViewById(R.id.sizeTextView)
        private val toppingsTextView: TextView = itemView.findViewById(R.id.toppingsTextView)
        private val noteTextView: TextView = itemView.findViewById(R.id.noteTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        private val editButton: ImageButton = itemView.findViewById(R.id.editItemButton)

        fun bind(item: OrderItem, position: Int) {
            // Định dạng tiền tệ
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            formatter.maximumFractionDigits = 0
            val price = formatter.format(item.unitPrice * item.quantity).replace("₫", "đ")

            // Hiển thị thông tin cơ bản
            quantityTextView.text = "x${item.quantity}"
            productNameTextView.text = item.productName

            // Hiển thị size
            if (item.size != null) {
                sizeTextView.visibility = View.VISIBLE
                sizeTextView.text = "Size: ${item.size.name}"
            } else {
                sizeTextView.visibility = View.GONE
            }

            // Hiển thị toppings
            if (item.toppings.isNotEmpty()) {
                toppingsTextView.visibility = View.VISIBLE
                val toppingNames = item.toppings.joinToString(", ") { it.name }
                toppingsTextView.text = "Topping: $toppingNames"
            } else {
                toppingsTextView.visibility = View.GONE
            }

            // Hiển thị ghi chú
            if (!item.note.isNullOrEmpty()) {
                noteTextView.visibility = View.VISIBLE
                noteTextView.text = "Ghi chú: ${item.note}"
            } else {
                noteTextView.visibility = View.GONE
            }

            priceTextView.text = price

            // Xử lý sự kiện khi nhấn nút sửa
            editButton.setOnClickListener {
                onEditItemClicked(item, position)
            }
        }
    }
}