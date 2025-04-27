package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.OrderRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RatingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    suspend fun rateOrder(orderId: String, rating: Int, comment: String): Boolean {
        return try {
            if (currentUserId.isEmpty()) {
                throw Exception("User not authenticated")
            }

            val orderRating = OrderRating(
                orderId = orderId,
                userId = currentUserId,
                rating = rating,
                comment = comment
            )

            // Check if rating already exists
            val existingRating = db.collection("ratings")
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("userId", currentUserId)
                .get()
                .await()

            if (existingRating.documents.isNotEmpty()) {
                // Update existing rating
                val ratingId = existingRating.documents[0].id
                db.collection("ratings").document(ratingId)
                    .update(
                        mapOf(
                            "rating" to rating,
                            "comment" to comment,
                            "timestamp" to System.currentTimeMillis()
                        )
                    ).await()
            } else {
                // Create new rating
                db.collection("ratings").add(orderRating).await()
            }

            true
        } catch (e: Exception) {
            Log.e("RatingRepository", "Error rating order: ${e.message}")
            false
        }
    }

    suspend fun getOrderRating(orderId: String): OrderRating? {
        return try {
            if (currentUserId.isEmpty()) return null

            val ratingDoc = db.collection("ratings")
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("userId", currentUserId)
                .get()
                .await()

            if (ratingDoc.documents.isEmpty()) return null

            val document = ratingDoc.documents[0]
            val rating = document.toObject(OrderRating::class.java)
            rating?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("RatingRepository", "Error getting order rating: ${e.message}")
            null
        }
    }
}