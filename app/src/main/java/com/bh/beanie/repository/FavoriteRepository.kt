package com.bh.beanie.repository

import com.bh.beanie.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FavoriteRepository(private val firestore: FirebaseFirestore) {
    private val usersFavoritesPath = "users/%s/favorites"

    fun addFavorite(userId: String, product: Product) {
        val formattedPath = String.format(usersFavoritesPath, userId)
        firestore.collection(formattedPath).document(product.id)
            .set(product)
    }

    fun removeFavorite(userId: String, productId: String) {
        val formattedPath = String.format(usersFavoritesPath, userId)
        firestore.collection(formattedPath).document(productId)
            .delete()
    }

    suspend fun getFavorites(userId: String): List<Product> {
        val formattedPath = String.format(usersFavoritesPath, userId)
        return try {
            val snapshot = firestore.collection(formattedPath)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isFavorite(userId: String, productId: String, callback: (Boolean) -> Unit) {
        val formattedPath = String.format(usersFavoritesPath, userId)
        firestore.collection(formattedPath).document(productId)
            .get()
            .addOnSuccessListener { callback(it.exists()) }
            .addOnFailureListener { callback(false) }
    }
}