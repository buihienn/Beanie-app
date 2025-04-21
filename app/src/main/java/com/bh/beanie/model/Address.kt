package com.bh.beanie.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    val id: String = "",
    val nameAddress: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val addressDetail: String = "",
    val isDefault: Boolean = false
) : Parcelable