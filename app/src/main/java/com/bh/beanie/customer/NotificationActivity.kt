package com.bh.beanie.customer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.NotificationAdapter
import com.bh.beanie.model.NotificationItem

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var toolbar: Toolbar // Thêm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        toolbar = findViewById(R.id.toolbar) // Ánh xạ Toolbar

        // Cấu hình Toolbar
        setSupportActionBar(toolbar) // Đặt Toolbar làm ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Hiện nút Back
        toolbar.setNavigationOnClickListener { onBackPressed() } // Xử lý sự kiện click nút Back

        // Tạo LayoutManager (danh sách dọc)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Tạo danh sách thông báo (placeholder data)
        val notifications = listOf(
            NotificationItem(R.drawable.ic_notification, "Welcome!", "Thank you for signing up.", "Just now"),
            NotificationItem(R.drawable.ic_notification, "New Offer!", "Get 50% off your next order.", "2 hours ago"),
            NotificationItem(R.drawable.ic_notification, "Order Shipped", "Your order #12345 has been shipped.", "Yesterday")
        )

        // Tạo Adapter và gắn vào RecyclerView
        adapter = NotificationAdapter(notifications)
        recyclerView.adapter = adapter
    }
}