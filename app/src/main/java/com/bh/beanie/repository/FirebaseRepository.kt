package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository(private val db: FirebaseFirestore) {

    fun fetchCategories(branchId: String, onSuccess: (List<Category>) -> Unit, onFailure: (Exception) -> Unit) {
        val categoriesRef = db.collection("branches").document(branchId).collection("categories")
        categoriesRef.get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.map { doc ->
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unnamed Category",
                        items = emptyList() // Items can be fetched separately if needed
                    )
                }
                onSuccess(categories)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error fetching categories", exception)
                onFailure(exception)
            }
    }

    fun fetchCategoryItems(branchId: String, categoryId: String, onSuccess: (List<CategoryItem>) -> Unit, onFailure: (Exception) -> Unit) {
        val productsRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId).collection("products")
        productsRef.get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.map { doc ->
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
                onSuccess(items)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error fetching category items", exception)
                onFailure(exception)
            }
    }

    fun addCategory(branchId: String, category: Category, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val categoriesRef = db.collection("branches").document(branchId).collection("categories")
        val categoryData = mapOf("name" to category.name)
        categoriesRef.document(category.id).set(categoryData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error adding category", exception)
                onFailure(exception)
            }
    }

    fun addCategoryItem(branchId: String, categoryId: String, item: CategoryItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val productsRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId).collection("products")
        val itemData = mapOf(
            "name" to item.name,
            "description" to item.description,
            "price" to item.price,
            "imageUrl" to item.imageUrl,
            "stock" to item.stockQuantity
        )
        productsRef.document(item.id).set(itemData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error adding category item", exception)
                onFailure(exception)
            }
    }

    fun editCategoryItem( branchId: String,  categoryId: String,
        item: CategoryItem, onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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

        itemRef.update(updatedData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error editing category item", exception)
                onFailure(exception)
            }
    }
}