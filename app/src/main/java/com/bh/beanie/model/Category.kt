package com.bh.beanie.model

data class Category(
    val id: String,
    val name: String,
    val items: List<CategoryItem>,
    val isExpanded: Boolean = false
)