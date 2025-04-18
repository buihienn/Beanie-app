package com.bh.beanie.model

import com.google.firebase.Timestamp

data class Voucher(
    val id: String = "",
    val name: String,
    val content: String,
    val expiryDate: Timestamp,
    val state: String,  // ACTIVE, EXPIRED, DISABLED
    val imageUrl: String,

    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double?
)