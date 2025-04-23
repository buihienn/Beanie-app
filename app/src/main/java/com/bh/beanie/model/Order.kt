package com.bh.beanie.model

import com.google.firebase.Timestamp
import java.util.Date

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
    val status: String = "WAITING ACCEPT", //"WAITING ACCEPT" "READY FOR PICKUP"  "PENDING" "DELIVERING" "COMPLETED" "CANCELLED"
    val orderTime: Timestamp = Timestamp.now(),
    val paymentMethod: String = "CASH", // "MOMO", "VNPAY", ...
    val note: String = ""
) {
    fun getOrderDate(): Date {
        return orderTime.toDate()
    }
}