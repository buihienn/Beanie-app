package com.bh.beanie.repository

import com.bh.beanie.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            // Only update these fields
            val updates = mapOf(
                "username" to updatedUser.username,
                "email" to updatedUser.email,
                "phone" to updatedUser.phone,
                "dob" to updatedUser.dob,
                "gender" to updatedUser.gender
            )

            usersCollection.document(currentUid).update(updates).await()

            // If email was updated, update the Firebase Authentication email
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
            // Delete user data from Firestore
            usersCollection.document(currentUid).delete().await()

            // Delete user authentication
            auth.currentUser?.delete()?.await()

            return@withContext true
        } catch (e: Exception) {
            throw e
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