package com.bh.beanie.model

import com.google.firebase.Timestamp

data class Voucher(
    var id: String = "",
    var name: String = "",
    var content: String = "",
    var discountValue: Double = 0.0,
    var discountType: String = "",
    var state: String = "",
    var expiryDate: Timestamp? = null,
    var minOrderAmount: Double = 0.0,
    var imageUrl: String = "",
    var redeemPoints: Int = 0,
)