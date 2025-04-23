package com.bh.beanie.model

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val size : ProductSize? = null,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val toppings: List<ProductTopping> = emptyList(),
    val note: String = ""
)