package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Category
import com.bh.beanie.model.Product
import com.google.firebase.firestore.DocumentSnapshot
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

    suspend fun fetchCategoriesPaginated(
        branchId: String,
        lastVisibleDocument: DocumentSnapshot? = null,
        pageSize: Int = 5
    ): Pair<List<Category>, DocumentSnapshot?> {
        Log.d("CategoryRepository", "Fetching categories with branchId: $branchId, lastVisible: ${lastVisibleDocument?.id}")

        var query = firestore.collection("branches").document(branchId)
            .collection("categories")
            .orderBy("name")
            .limit(pageSize.toLong())

        lastVisibleDocument?.let {
            query = query.startAfter(it)
        }

        val snapshot = query.get().await()

        val categories = snapshot.documents.map { doc ->
            Category(
                id = doc.id,
                name = doc.getString("name") ?: "",
                items = emptyList()
            )
        }
        Log.d("CategoryRepository", "Fetched ${categories.size} categories: ${categories.map { "${it.id}:${it.name}" }}")

        val lastDoc = snapshot.documents.lastOrNull()

        return Pair(categories, lastDoc)
    }

}