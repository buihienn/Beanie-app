package com.bh.beanie.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MembershipLevel(
    val level: String = "",
    val pointsRequired: Int = 0,
    val rewards: List<Reward> = emptyList()
)

@IgnoreExtraProperties
data class Reward(
    val content: String = "",
    val imageUrl: String = "",
    val name: String = ""
)
