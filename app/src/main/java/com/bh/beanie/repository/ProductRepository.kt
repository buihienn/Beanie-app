package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductTopping
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.String

class ProductRepository(private val db: FirebaseFirestore) {
    // Lấy thông tin sản phẩm
    suspend fun fetchProduct(branchId: String, categoryId: String, productId: String): Product? {
        try {
            val productRef = db.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .collection("products").document(productId)
            val doc = productRef.get().await()

            if (!doc.exists()) {
                return null
            }

            return Product(
                id = doc.id,
                name = doc.getString("name") ?: "Unnamed Product",
                description = doc.getString("description") ?: "No description",
                price = doc.getDouble("price") ?: 0.0,
                imageUrl = doc.getString("imageUrl") ?: "",
                stockQuantity = doc.getLong("stock")?.toInt() ?: 0,
                categoryId = categoryId,
                size = getSizeMap(doc),
                toppingsAvailable = getToppingList(doc)
            )
        } catch (e: Exception) {
            Log.e("ProductRepo", "Lỗi khi lấy thông tin sản phẩm: ${e.message}", e)
            return null
        }
    }

    private fun getSizeMap(doc: DocumentSnapshot): Map<String, Double> {
        return try {
            val sizesData = doc.get("size")
            if (sizesData is Map<*, *>) {
                sizesData.entries.mapNotNull { entry ->
                    val key = entry.key as? String ?: return@mapNotNull null
                    val value = when (val v = entry.value) {
                        is Double -> v
                        is Long -> v.toDouble()
                        is Int -> v.toDouble()
                        is Number -> v.toDouble()
                        else -> return@mapNotNull null
                    }
                    key to value
                }.toMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("ProductRepo", "Lỗi khi đọc size: ${e.message}", e)
            emptyMap()
        }
    }

    private fun getToppingList(doc: DocumentSnapshot): List<String> {
        return try {
            val toppingsData = doc.get("toppings")
            if (toppingsData is List<*>) {
                toppingsData.filterIsInstance<String>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ProductRepo", "Lỗi khi đọc toppings: ${e.message}", e)
            emptyList()
        }
    }

    // Lấy thông tin chi tiết các topping của sản phẩm
    suspend fun fetchProductToppings(branchId: String, product: Product): List<ProductTopping> {
        try {
            // Nếu sản phẩm không có topping nào
            if (product.toppingsAvailable.isEmpty()) {
                return emptyList()
            }

            // Lấy chi tiết các topping dựa trên danh sách ID
            return fetchToppingsByIds(branchId, product.toppingsAvailable)
        } catch (e: Exception) {
            println("Lỗi khi lấy chi tiết toppings của sản phẩm: ${e.message}")
            return emptyList()
        }
    }

    // Lấy thông tin chi tiết của các topping dựa trên danh sách ID
    suspend fun fetchToppingsByIds(branchId: String, toppingIds: List<String>): List<ProductTopping> {
        if (toppingIds.isEmpty()) return emptyList()

        try {
            // Tạo danh sách kết quả
            val toppings = mutableListOf<ProductTopping>()

            // Lấy toàn bộ toppings của chi nhánh
            val snapshot = db.collection("branches").document(branchId)
                .collection("toppings")
                .get()
                .await()

            // Lọc các toppings có ID nằm trong danh sách toppingIds
            for (doc in snapshot.documents) {
                if (doc.id in toppingIds) {
                    val topping = ProductTopping(
                        id = doc.id,
                        name = doc.getString("name") ?: "Topping không tên",
                        price = doc.getDouble("price") ?: 0.0,
//                        stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 0
                    )
                    toppings.add(topping)
                }
            }

            return toppings
        } catch (e: Exception) {
            println("Lỗi khi lấy thông tin chi tiết toppings: ${e.message}")
            return emptyList()
        }
    }

    suspend fun fetchProductsPaginated(
        branchId: String,
        categoryId: String,
        lastVisibleDocument: DocumentSnapshot? = null,
        pageSize: Int = 4
    ): Pair<List<Product>, DocumentSnapshot?> {
        var query = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products")
            .limit(pageSize.toLong())

        lastVisibleDocument?.let {
            query = query.startAfter(it)
        }

        val snapshot = query.get().await()

        val products = snapshot.documents.map { doc ->
            Product(
                id = doc.id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                imageUrl = doc.getString("imageUrl") ?: "",
                stockQuantity = doc.getLong("stock")?.toInt() ?: 0,
                categoryId = categoryId,
                size = emptyMap(), // Lấy đầy đủ thông tin nếu cần
                toppingsAvailable = emptyList()
            )
        }

        val lastDoc = snapshot.documents.lastOrNull()

        return Pair(products, lastDoc)
    }

    suspend fun fetchProductsForCategory(branchId: String, categoryId: String, limit: Int = 1): List<Product> {
        return try {
            val snapshot = db.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .collection("products")
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Product(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
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

    suspend fun searchProducts(branchId: String, query: String): List<Product> {
        val searchResults = mutableListOf<Product>()

        try {
            // Tìm kiếm qua tất cả categories
            val categoriesRef = db.collection("branches").document(branchId).collection("categories")
            val categoriesSnapshot = categoriesRef.get().await()

            for (categoryDoc in categoriesSnapshot.documents) {
                val categoryId = categoryDoc.id

                // Tìm kiếm sản phẩm trong category này
                val productsRef = db.collection("branches").document(branchId)
                    .collection("categories").document(categoryId)
                    .collection("products")

                val productsSnapshot = productsRef.get().await()

                for (productDoc in productsSnapshot.documents) {
                    val productName = productDoc.getString("name") ?: ""
                    val productDescription = productDoc.getString("description") ?: ""

                    // Kiểm tra xem query có match với tên hoặc mô tả không
                    if (productName.contains(query, ignoreCase = true) ||
                        productDescription.contains(query, ignoreCase = true)) {

                        val product = Product(
                            id = productDoc.id,
                            name = productName,
                            description = productDescription,
                            price = productDoc.getDouble("price") ?: 0.0,
                            imageUrl = productDoc.getString("imageUrl") ?: "",
                            stockQuantity = productDoc.getLong("stock")?.toInt() ?: 0,
                            categoryId = categoryId
                        )
                        searchResults.add(product)
                    }
                }
            }

            return searchResults
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun fetchBestSellersSuspend(branchId: String): List<Product> {
        val bestSellersList = mutableListOf<Product>()

        try {
            // Nếu không tìm thấy, thử tìm trong từng category
            val categoriesSnapshot = db.collection("branches")
                .document(branchId)
                .collection("categories")
                .get()
                .await()

            for (categoryDoc in categoriesSnapshot.documents) {
                val categoryId = categoryDoc.id

                val productsSnapshot = db.collection("branches")
                    .document(branchId)
                    .collection("categories")
                    .document(categoryId)
                    .collection("products")
                    .whereEqualTo("isBestSeller", true)
                    .get()
                    .await()

                for (productDoc in productsSnapshot.documents) {
                    val product = Product(
                        id = productDoc.id,
                        name = productDoc.getString("name") ?: "",
                        description = productDoc.getString("description") ?: "",
                        price = productDoc.getDouble("price") ?: 0.0,
                        imageUrl = productDoc.getString("imageUrl") ?: "",
                        stockQuantity = productDoc.getLong("stock")?.toInt() ?: 0,
                        categoryId = categoryId
                    )
                    bestSellersList.add(product)
                }
            }

            return bestSellersList
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error fetching best sellers: ${e.message}")
            return emptyList()
        }
    }
}