package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    suspend fun addNotification(notification: com.bh.beanie.model.Notification): String {
        return try {
            val db = FirebaseFirestore.getInstance()
            val notificationData = hashMapOf(
                "userId" to notification.userId,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type,
                "read" to notification.read,
                "timestamp" to notification.timestamp,
                "data" to notification.data
            )

            val documentRef = db.collection("notifications").add(notificationData).await()
            documentRef.id
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error adding notification", e)
            throw e
        }
    }

    fun getUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val subscription = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to recent notifications
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    trySend(notifications)
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId)
                .update("read", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking notification as read", e)
            false
        }
    }

    suspend fun markAllAsRead(userId: String): Boolean {
        return try {
            val batch = db.batch()
            val unreadNotifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            for (doc in unreadNotifications.documents) {
                batch.update(doc.reference, "read", true)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking all notifications as read", e)
            false
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val querySnapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            querySnapshot.documents.size
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error getting unread count", e)
            0
        }
    }

    suspend fun createNotification(notification: Notification): String? {
        return try {
            val docRef = notificationsCollection.add(notification).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error creating notification", e)
            null
        }
    }
}