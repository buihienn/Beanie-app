package com.bh.beanie.model

data class Product (
    var id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val categoryId: String,
    val size : Map<String, Double> = emptyMap(), // M : gia, S: gia, L: gia
    val toppingsAvailable: List<String> = emptyList() // Lưu id
)