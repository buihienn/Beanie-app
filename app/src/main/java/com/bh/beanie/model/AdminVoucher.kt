package com.bh.beanie.model

data class AdminVoucher(
    val name: String,
    val content: String,
    val expiryDate: String,
    val state: String,
    val imageUrl: String
)
