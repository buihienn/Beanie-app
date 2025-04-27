package com.bh.beanie.model

data class OrderRating(
    val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val rating: Int = 0,  // 1-5 stars
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)