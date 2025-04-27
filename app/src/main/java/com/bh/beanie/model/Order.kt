package com.bh.beanie.model

import com.google.firebase.Timestamp

data class Order(
    var id: String = "",
    var branchId: String = "",
    var userId: String = "",
    var customerName: String = "",
    var phoneNumber: String = "",
    var deliveryAddress: String = "",
    var type: String = "DELIVERY", // "DELIVERY", "TAKEAWAY"


    var items: List<OrderItem> = emptyList(),
    var totalPrice: Double = 0.0,
    var status: String = "WAITING ACCEPT", //"WAITING ACCEPT" "READY FOR PICKUP"  "PENDING" "DELIVERING" "COMPLETED" "CANCELLED"
    var orderTime: Timestamp = Timestamp.now(),
    var note: String = "",

    var paymentMethod: String = "CASH", // "MOMO", "VNPAY", ...
    var transactionId: String = "",
)