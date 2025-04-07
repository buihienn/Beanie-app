package com.bh.beanie.user.model

data class Address(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val addressDetail: String,
    val isDefault: Boolean = false
)