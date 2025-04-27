package com.bh.beanie.model

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val categoryId: String = "",
    val size : String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val toppings: List<ProductTopping> = emptyList(),
    val note: String = ""
)