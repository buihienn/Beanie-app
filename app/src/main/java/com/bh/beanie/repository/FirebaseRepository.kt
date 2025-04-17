package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val db: FirebaseFirestore) {

    suspend fun fetchCategoriesSuspend(branchId: String): List<Category> {
        val categoriesRef = db.collection("branches").document(branchId).collection("categories")
        val snapshot = categoriesRef.get().await()

        return snapshot.map { doc ->
            Category(
                id = doc.id,
                name = doc.getString("name") ?: "Unnamed Category",
                items = emptyList()
            )
        }
    }

    suspend fun fetchCategoryItemsSuspend(branchId: String, categoryId: String): List<CategoryItem> {
        val productsRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId).collection("products")
        val snapshot = productsRef.get().await()

        return snapshot.map { doc ->
            CategoryItem(
                id = doc.id,
                name = doc.getString("name") ?: "Unnamed Product",
                description = doc.getString("description") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                imageUrl = doc.getString("imageUrl") ?: "",
                stockQuantity = doc.getLong("stock")?.toInt() ?: 0,
                categoryId = categoryId
            )
        }
    }

    suspend fun addCategorySuspend(branchId: String, category: Category) {
        val categoriesRef = db.collection("branches").document(branchId).collection("categories")
        val categoryData = mapOf("name" to category.name)
        categoriesRef.document(category.id).set(categoryData).await()
    }

    suspend fun addCategoryItemSuspend(branchId: String, categoryId: String, item: CategoryItem) {
        val productsRef = db.collection("branches")
            .document(branchId)
            .collection("categories")
            .document(categoryId)
            .collection("products")
            .document(item.id)

        val itemData = mapOf(
            "name" to item.name,
            "description" to item.description,
            "price" to item.price,
            "imageUrl" to item.imageUrl,
            "stock" to item.stockQuantity
        )

        productsRef.set(itemData).await()
    }

    suspend fun editCategoryItemSuspend(branchId: String, categoryId: String, item: CategoryItem) {
        val itemRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(item.id)

        val updatedData = mapOf(
            "name" to item.name,
            "description" to item.description,
            "price" to item.price,
            "imageUrl" to item.imageUrl,
            "stock" to item.stockQuantity
        )

        itemRef.update(updatedData).await()
    }

    suspend fun deleteCategoryItemSuspend(branchId: String, categoryId: String, itemId: String) {
        val itemRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(itemId)

        itemRef.delete().await()
    }
}