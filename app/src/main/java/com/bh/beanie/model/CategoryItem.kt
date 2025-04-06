package com.bh.beanie.model


// It a child of category
data class CategoryItem (
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val categoryId: String
)