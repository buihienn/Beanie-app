package com.bh.beanie.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val branchId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val phoneNumber: String = "",
    val deliveryAddress: String = "",
    val type: String = "DELIVERY", // "DELIVERY", "TAKEAWAY"


    val items: List<OrderItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val status: String = "PENDING", // "PENDING", "CONFIRMED", "DELIVERED", "CANCELLED"
    val orderTime: Timestamp = Timestamp.now(),
    val paymentMethod: String = "CASH", // "MOMO", "VNPAY", ...
    val note: String = ""
)