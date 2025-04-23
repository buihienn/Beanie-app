package com.bh.beanie.model

data class Product (
    var id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val categoryId: String,
    val sizesAvailable: List<ProductSize> = emptyList(),
    val toppingsAvailable: List<String> = emptyList() // Lưu id
)