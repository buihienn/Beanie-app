package com.bh.beanie.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserVoucher(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val voucherId: String = "",
    val used: Boolean = false,
    @ServerTimestamp
    val acquiredDate: Timestamp? = null,
    val usedDate: Timestamp? = null
)