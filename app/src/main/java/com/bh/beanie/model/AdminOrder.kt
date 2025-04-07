package com.bh.beanie.model

data class AdminOrder(
    val id: String,
    val customerName: String,
    val orderTime: String,
    val status: String,
    val imgUrl: String
)
