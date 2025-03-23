package com.bh.beanie.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.R
import com.google.android.material.textfield.TextInputEditText

data class OrderItem(
    val name: String,
    val quantity: Int,
    val price: Double
)
class OrderReviewActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderType: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var etNote: TextInputEditText
    private lateinit var btnSendFeedback: Button
    private lateinit var llOrderItems: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_review)

        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderType = findViewById(R.id.tvOrderType)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        ratingBar = findViewById(R.id.ratingBar)
        etNote = findViewById(R.id.etNote)
        btnSendFeedback = findViewById(R.id.btnSendFeedback)
        llOrderItems = findViewById(R.id.llOrderItems)


        // Lấy thông tin đơn hàng từ Intent (hoặc từ database, SharedPreferences, ...)
        // Đây là ví dụ, bạn cần thay thế bằng dữ liệu thật
        val orderId = intent.getStringExtra("ORDER_ID") ?: "#12345"
        val orderType = intent.getStringExtra("ORDER_TYPE") ?: "Dine-in"
        val orderDate = intent.getStringExtra("ORDER_DATE") ?: "2023-11-20"
        //Danh sách item:
        val orderItems = listOf(
            OrderItem("Burger", 2, 10.99),
            OrderItem("Fries", 1, 3.99),
            OrderItem("Coke", 2, 1.99)
        )

        // Hiển thị thông tin đơn hàng
        tvOrderId.text = orderId
        tvOrderType.text = orderType
        tvOrderDate.text = orderDate
        //Thêm các item vào LinearLayout:
        for (item in orderItems) {
            addItemView(item)
        }


        // Xử lý sự kiện click cho nút Send Feedback
        btnSendFeedback.setOnClickListener {
            sendFeedback()
        }
    }
    private fun addItemView(item: OrderItem) {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.order_item_layout, llOrderItems, false)

        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvItemQuantity: TextView = itemView.findViewById(R.id.tvItemQuantity)
        val tvItemPrice: TextView = itemView.findViewById(R.id.tvItemPrice)

        tvItemName.text = item.name
        tvItemQuantity.text = "x${item.quantity}"
        tvItemPrice.text = String.format("$%.2f", item.price * item.quantity)

        llOrderItems.addView(itemView)
    }

    private fun sendFeedback() {
        val rating = ratingBar.rating
        val note = etNote.text.toString().trim()

        // TODO: Gửi đánh giá và ghi chú lên server (gọi API)

        Toast.makeText(this, "Feedback sent! Rating: $rating, Note: $note", Toast.LENGTH_SHORT).show()
        finish() // Đóng OrderReviewActivity
    }
}