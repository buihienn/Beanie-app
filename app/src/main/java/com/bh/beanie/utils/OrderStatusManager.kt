package com.bh.beanie.utils

import com.bh.beanie.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object OrderStatusManager {
    private var listenerRegistration: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var currentOrder: Order? = null

    fun startListening(onOrderUpdate: (Order?) -> Unit) {
        stopListening()

        if (userId.isEmpty()) {
            onOrderUpdate(null)
            return
        }

        listenerRegistration = db.collection("orders")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("PENDING", "WAITING ACCEPT", "READY FOR PICKUP", "DELIVERING"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onOrderUpdate(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    val order = document.toObject(Order::class.java)?.copy(id = document.id)
                    currentOrder = order
                    onOrderUpdate(order)
                } else {
                    currentOrder = null
                    onOrderUpdate(null)
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
