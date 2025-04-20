package com.bh.beanie.model

data class Address(
    val id: Int,
    val nameAddress: String,
    val name: String,
    val phoneNumber: String,
    val addressDetail: String,
    val isDefault: Boolean = false
)