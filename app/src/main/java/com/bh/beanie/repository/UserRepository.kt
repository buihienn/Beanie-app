package com.bh.beanie.repository

import com.bh.beanie.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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

            val lastResetCalendar = Calendar.getInstance().apply { time = lastReset.toDate() }
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
