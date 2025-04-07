package com.bh.beanie.user.model

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val imageResourceId: Int,
    var isFavorite: Boolean = false
)