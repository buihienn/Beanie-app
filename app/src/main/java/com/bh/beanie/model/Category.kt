package com.bh.beanie.model

data class Category(
    val id: Int,
    val name: String,
    val items: List<CategoryItem>,
    val isExpanded: Boolean = false
)