package com.bh.beanie.repository

import com.bh.beanie.model.Category
import com.bh.beanie.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository(private val firestore: FirebaseFirestore) {

    suspend fun fetchCategories(branchId: String): List<Category> {
        return try {
            val categoriesRef = firestore.collection("branches").document(branchId).collection("categories")
            val snapshot = categoriesRef.get().await()

            snapshot.documents.map { doc ->
                Category(
                    id = doc.id,
                    name = doc.getString("name") ?: "Unnamed Category",
                    items = emptyList()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCategoryItems(branchId: String, categoryId: String): List<Product> {
        return try {
            val productsRef = firestore.collection("branches").document(branchId)
                .collection("categories").document(categoryId).collection("products")
            val snapshot = productsRef.get().await()

            snapshot.documents.map { doc ->
                Product(
                    id = doc.id,
                    name = doc.getString("name") ?: "Unnamed Product",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    stockQuantity = doc.getLong("stock")?.toInt() ?: 0,
                    categoryId = categoryId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCategoryWithItems(branchId: String, categoryId: String): Category? {
        return try {
            val categoryDoc = firestore.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .get().await()

            if (!categoryDoc.exists()) return null

            val items = fetchCategoryItems(branchId, categoryId)

            Category(
                id = categoryDoc.id,
                name = categoryDoc.getString("name") ?: "Unnamed Category",
                items = items
            )
        } catch (e: Exception) {
            null
        }
    }
}