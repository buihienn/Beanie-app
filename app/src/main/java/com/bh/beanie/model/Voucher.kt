package com.bh.beanie.model

import com.google.firebase.Timestamp

data class Voucher(
    val id: String = "",
    val name: String = "",
    val content: String = "",
    val discountValue: Double = 0.0,
    val discountType: String = "",
    val state: String = "",
    val expiryDate: com.google.firebase.Timestamp? = null,
    val minOrderAmount: Double = 0.0,
    val imageUrl: String = ""
)