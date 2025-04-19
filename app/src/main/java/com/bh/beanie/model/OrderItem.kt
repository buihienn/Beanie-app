package com.bh.beanie.model

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val size : String = "", // "SMALL", "MEDIUM", "LARGE"
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
)