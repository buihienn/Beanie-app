package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
//import com.google.firebase.firestore.toDate
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext null
        try {
            val snapshot = usersCollection.document(currentUid).get().await()
            return@withContext snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateUserProfile(updatedUser: User): Boolean = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val updates = mapOf(
                "username" to updatedUser.username,
                "email" to updatedUser.email,
                "phone" to updatedUser.phone,
                "dob" to updatedUser.dob,
                "gender" to updatedUser.gender
            )

            usersCollection.document(currentUid).update(updates).await()

            val currentEmail = auth.currentUser?.email
            if (currentEmail != updatedUser.email) {
                auth.currentUser?.updateEmail(updatedUser.email)?.await()
            }

            return@withContext true
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteAccount(): Boolean = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext false
        try {
            usersCollection.document(currentUid).delete().await()
            auth.currentUser?.delete()?.await()
            return@withContext true
        } catch (e: Exception) {
            throw e
        }
    }

    fun checkAndResetPointsIfNeeded(userId: String) {
        val userRef = usersCollection.document(userId)

        userRef.get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java) ?: return@addOnSuccessListener

            val lastReset = user.lastPointReset
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)

            if (lastReset == null) {
                if (currentMonth >= Calendar.JUNE) {
                    userRef.update(
                        mapOf(
                            "presentPoints" to 0,
                            "lastPointReset" to Timestamp.now()
                        )
                    )
                }
                return@addOnSuccessListener
            }

            val lastResetCalendar = Calendar.getInstance().apply { time = lastReset }
            val lastResetYear = lastResetCalendar.get(Calendar.YEAR)

            if (currentYear > lastResetYear && currentMonth >= Calendar.JUNE) {
                userRef.update(
                    mapOf(
                        "presentPoints" to 0,
                        "lastPointReset" to Timestamp.now()
                    )
                )
            }
        }
    }


    suspend fun getUserMembershipInfo(userId: String): Map<String, Any?>? = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection.document(userId).get().await()
            if (snapshot.exists()) {
                val membershipLevel = snapshot.getString("membershipLevel")
                val points = snapshot.getLong("points")?.toInt()
                val presentPoints = snapshot.getLong("presentPoints")?.toInt()

                return@withContext mapOf(
                    "membershipLevel" to membershipLevel,
                    "points" to points,
                    "presentPoints" to presentPoints
                )
            }
            return@withContext null
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateMembershipLevel(userId: String, loyalPointsRequired: Int, vipPointsRequired: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val points = snapshot.getLong("points")?.toInt() ?: 0
                val newMembershipLevel = when {
                    points >= vipPointsRequired -> "VIP"
                    points >= loyalPointsRequired -> "Loyal"
                    else -> "New"
                }

                userRef.update("membershipLevel", newMembershipLevel).await()
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getUserLastSpinDate(userId: String): Date? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val timestamp = userDoc.getTimestamp("lastSpinDate")
            timestamp?.toDate()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting last spin date: ${e.message}")
            null
        }
    }

    suspend fun updateUserLastSpinDate(userId: String) {
        try {
            val currentDate = Date()
            usersCollection.document(userId)
                .update("lastSpinDate", Timestamp(currentDate))
                .await()
            Log.d("UserRepository", "Last spin date updated for user: $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating last spin date: ${e.message}")
            throw e
        }
    }


    suspend fun canUserSpinToday(userId: String): Boolean {
        val lastSpinDate = getUserLastSpinDate(userId)
        if (lastSpinDate == null) {
            return true // User never spun before
        }

        // Check if last spin was on a different day
        val calendar = Calendar.getInstance()

        // Get today's date with time set to 00:00:00
        val today = calendar.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        // Get last spin date with time set to 00:00:00
        val lastSpinCalendar = calendar.clone() as Calendar
        lastSpinCalendar.time = lastSpinDate
        lastSpinCalendar.set(Calendar.HOUR_OF_DAY, 0)
        lastSpinCalendar.set(Calendar.MINUTE, 0)
        lastSpinCalendar.set(Calendar.SECOND, 0)
        lastSpinCalendar.set(Calendar.MILLISECOND, 0)

        // Compare dates by checking if the time in milliseconds is different
        return today.timeInMillis > lastSpinCalendar.timeInMillis
    }

    suspend fun updateUserPoints(userId: String, orderTotal: Double): Boolean {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: return false

            // Tính số điểm được cộng (1 điểm cho mỗi 1000đ)
            val earnedPoints = (orderTotal / 1000).toInt()

            // Cập nhật điểm
            val currentPoints = user.points
            val currentPresentPoints = user.presentPoints

            // Cập nhật cả tổng điểm (points) và điểm hiện tại (presentPoints)
            usersCollection.document(userId).update(
                mapOf(
                    "points" to currentPoints + earnedPoints,
                    "presentPoints" to currentPresentPoints + earnedPoints
                )
            ).await()

            // Kiểm tra và cập nhật cấp thành viên nếu cần
            updateMembershipLevel(userId, currentPoints + earnedPoints)

            Log.d("UserRepository", "Updated points for user $userId: +$earnedPoints points")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user points", e)
            false
        }
    }

    private suspend fun updateMembershipLevel(userId: String, totalPoints: Int) {
        try {
            val newLevel = when {
                totalPoints >= 1500 -> "VIP"
                totalPoints >= 500 -> "LOYAL"
                else -> "NEW"
            }

            usersCollection.document(userId).update("membershipLevel", newLevel).await()
            Log.d("UserRepository", "Updated membership level for user $userId to $newLevel")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating membership level", e)
        }
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository().also { instance = it }
            }
        }
    }
}
