package com.bh.beanie.repository

import com.bh.beanie.model.MembershipLevel
import com.bh.beanie.model.Reward
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MembershipRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchMembershipsSuspend(): List<MembershipLevel> {
        val membershipsRef = db.collection("memberships")
        val snapshot = membershipsRef.get().await()

        return snapshot.map { doc ->
            val level = doc.getString("level") ?: "Unknown"
            val pointsRequired = doc.getLong("pointsRequired")?.toInt() ?: 0

            val rewardsData = doc.get("rewards") as? List<Map<String, Any>> ?: emptyList()
            val rewards = rewardsData.map { rewardMap ->
                Reward(
                    content = rewardMap["content"] as? String ?: "",
                    imageUrl = rewardMap["imageUrl"] as? String ?: "",
                    name = rewardMap["name"] as? String ?: ""
                )
            }

            MembershipLevel(
                level = level,
                pointsRequired = pointsRequired,
                rewards = rewards
            )
        }
    }

    suspend fun fetchMembershipByLevel(level: String): MembershipLevel? {
        val membershipsRef = db.collection("memberships")
        val snapshot = membershipsRef
            .whereEqualTo("level", level)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null

        val pointsRequired = doc.getLong("pointsRequired")?.toInt() ?: 0
        val rewardsData = doc.get("rewards") as? List<Map<String, Any>> ?: emptyList()
        val rewards = rewardsData.map { rewardMap ->
            Reward(
                content = rewardMap["content"] as? String ?: "",
                imageUrl = rewardMap["imageUrl"] as? String ?: "",
                name = rewardMap["name"] as? String ?: ""
            )
        }

        return MembershipLevel(
            level = doc.getString("level") ?: "Unknown",
            pointsRequired = pointsRequired,
            rewards = rewards
        )
    }

    suspend fun getPointsRequiredForLevel(level: String): Int? {
        return try {
            val membershipsRef = db.collection("memberships")
            val snapshot = membershipsRef
                .whereEqualTo("level", level)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            doc?.getLong("pointsRequired")?.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
