package com.bh.beanie.repository

import com.bh.beanie.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository(private val firestore: FirebaseFirestore) {

    suspend fun fetchProductsSuspend(branchId: String): List<Product> {
        return try {
            val snapshot = firestore.collection("branches/$branchId/products")
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchProductsByCategorySuspend(branchId: String, categoryId: String): List<Product> {
        return try {
            // Sử dụng cấu trúc giống như trong FirebaseRepository
            val snapshot = firestore.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .collection("products")
                .get().await()

            snapshot.documents.mapNotNull {
                Product(
                    id = it.id,
                    name = it.getString("name") ?: "Unnamed Product",
                    description = it.getString("description") ?: "",
                    price = it.getDouble("price") ?: 0.0,
                    imageUrl = it.getString("imageUrl") ?: "",
                    stockQuantity = it.getLong("stock")?.toInt() ?: 0,
                    categoryId = categoryId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchBestSellersSuspend(branchId: String): List<Product> {
        return try {
            val snapshot = firestore.collection("branches/$branchId/products")
                .whereEqualTo("isBestSeller", true)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductById(branchId: String, productId: String, categoryId: String? = null): Product? {
        return try {
            val document = if (categoryId != null) {
                // Sử dụng đường dẫn cụ thể nếu biết categoryId
                firestore.collection("branches").document(branchId)
                    .collection("categories").document(categoryId)
                    .collection("products").document(productId)
                    .get().await()
            } else {
                // Sử dụng đường dẫn chung nếu không biết categoryId
                firestore.document("branches/$branchId/products/$productId")
                    .get().await()
            }

            if (document.exists()) {
                Product(
                    id = document.id,
                    name = document.getString("name") ?: "Unnamed Product",
                    description = document.getString("description") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    imageUrl = document.getString("imageUrl") ?: "",
                    stockQuantity = document.getLong("stock")?.toInt() ?: 0,
                    categoryId = categoryId ?: document.getString("categoryId") ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addProduct(branchId: String, categoryId: String, product: Product) {
        val productsRef = firestore.collection("branches")
            .document(branchId)
            .collection("categories")
            .document(categoryId)
            .collection("products")
            .document(product.id.ifEmpty { firestore.collection("products").document().id })

        val productData = mapOf(
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "imageUrl" to product.imageUrl,
            "stock" to product.stockQuantity
        )

        productsRef.set(productData).await()
    }

    suspend fun updateProduct(branchId: String, categoryId: String, product: Product) {
        val productRef = firestore.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(product.id)

        val updatedData = mapOf(
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "imageUrl" to product.imageUrl,
            "stock" to product.stockQuantity
        )

        productRef.update(updatedData).await()
    }

    suspend fun deleteProduct(branchId: String, categoryId: String, productId: String) {
        val productRef = firestore.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(productId)

        productRef.delete().await()
    }

    // Thêm hàm để đánh dấu sản phẩm là best seller
    suspend fun markAsBestSeller(branchId: String, categoryId: String, productId: String, isBestSeller: Boolean) {
        val productRef = firestore.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(productId)

        productRef.update("isBestSeller", isBestSeller).await()
    }

    // Thêm hàm để cập nhật số lượng tồn kho
    suspend fun updateStockQuantity(branchId: String, categoryId: String, productId: String, newQuantity: Int) {
        val productRef = firestore.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(productId)

        productRef.update("stock", newQuantity).await()
    }
}