package com.bh.beanie.model

data class Notification(
    val id: String = "", // Firestore document ID
    val title: String = "",
    val message: String = "",
    val type: String = "", // e.g., "VOUCHER", "ORDER", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val userId: String = "", // ID of the user this notification belongs to
    val data: Map<String, String> = mapOf() // Additional data for specific notification types
)