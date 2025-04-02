package com.bh.beanie.model

data class NotificationItem(
    val iconResId: Int,  // Resource ID của icon (ví dụ: R.drawable.ic_notification)
    val title: String,
    val content: String,
    val time: String
)