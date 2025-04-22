package com.bh.beanie.repository

import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductSize
import com.bh.beanie.model.ProductTopping
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.text.get

class ProductRepository(private val firestore: FirebaseFirestore) {
    // Lấy thông tin đầy đủ của sản phẩm bao gồm cả sizes và toppings
    suspend fun fetchCompleteProduct(branchId: String, categoryId: String, productId: String): Product? {
        try {
            // Lấy dữ liệu cơ bản của sản phẩm
            val productDoc = firestore.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .collection("products").document(productId)
                .get().await()

            if (!productDoc.exists()) return null

            // Tạo đối tượng sản phẩm với thông tin cơ bản
            val baseProduct = Product(
                id = productDoc.id,
                name = productDoc.getString("name") ?: "Sản phẩm chưa đặt tên",
                description = productDoc.getString("description") ?: "",
                price = productDoc.getDouble("price") ?: 0.0,
                imageUrl = productDoc.getString("imageUrl") ?: "",
                stockQuantity = productDoc.getLong("stock")?.toInt() ?: 0,
                categoryId = categoryId,
                sizesAvailable = emptyList(),
                toppingsAvailable = emptyList()
            )

            // Lấy danh sách kích thước của sản phẩm (lưu dưới dạng sub-collection)
            val sizes = fetchProductSizes(branchId, categoryId, productId)

            // Lấy danh sách ID của các topping từ tài liệu sản phẩm
            val toppings = productDoc.get("toppings") as? List<String> ?: emptyList()

            // Trả về sản phẩm đầy đủ với sizes và toppings
            return baseProduct.copy(
                sizesAvailable = sizes,
                toppingsAvailable = toppings // id
            )
        } catch (e: Exception) {
            // Ghi log lỗi
            println("Lỗi khi lấy thông tin sản phẩm: ${e.message}")
            return null
        }
    }

    // Lấy danh sách kích thước của sản phẩm
    private suspend fun fetchProductSizes(branchId: String, categoryId: String, productId: String): List<ProductSize> {
        return try {
            val sizesSnapshot = firestore.collection("branches").document(branchId)
                .collection("categories").document(categoryId)
                .collection("products").document(productId)
                .collection("sizes")
                .get().await()

            sizesSnapshot.documents.mapNotNull {
                it.toObject(ProductSize::class.java)?.copy(id = it.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Lấy danh sách sản phẩm bán chạy nhất
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
            val snapshot = firestore.collection("branches").document(branchId)
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

    suspend fun getProductById(productId: String): Product? {
        return try {
            android.util.Log.d("ProductRepo", "Fetching product with ID: $productId")

            // First try to find the product by searching through all branches and categories
            val branchesSnapshot = firestore.collection("branches").get().await()

            for (branchDoc in branchesSnapshot.documents) {
                val branchId = branchDoc.id
                android.util.Log.d("ProductRepo", "Searching in branch: $branchId")

                val categoriesSnapshot = firestore.collection("branches")
                    .document(branchId)
                    .collection("categories")
                    .get()
                    .await()

                for (categoryDoc in categoriesSnapshot.documents) {
                    val categoryId = categoryDoc.id
                    android.util.Log.d("ProductRepo", "Searching in category: $categoryId")

                    // Check if product exists in this category
                    val productDoc = firestore.collection("branches")
                        .document(branchId)
                        .collection("categories")
                        .document(categoryId)
                        .collection("products")
                        .document(productId)
                        .get()
                        .await()

                    if (productDoc.exists()) {
                        android.util.Log.d("ProductRepo", "Found product in branch: $branchId, category: $categoryId")

                        try {
                            // Trích xuất dữ liệu cẩn thận để tránh null
                            val name = productDoc.getString("name") ?: "Sản phẩm không tên"
                            val description = productDoc.getString("description") ?: ""
                            val price = productDoc.getDouble("price") ?: 0.0
                            val imageUrl = productDoc.getString("imageUrl") ?: ""
                            val stock = productDoc.getLong("stock")?.toInt() ?: 0

                            // Tạo đối tượng Product thủ công thay vì dùng toObject
                            val product = Product(
                                id = productId,
                                name = name,
                                description = description,
                                price = price,
                                imageUrl = imageUrl,
                                stockQuantity = stock,
                                categoryId = categoryId
                            )

                            // Get sizes
                            val sizes = fetchProductSizes(branchId, categoryId, productId)

                            // Get topping IDs - bảo vệ khỏi cast exception
                            val toppingsData = productDoc.get("toppings")
                            val toppings = if (toppingsData is List<*>) {
                                toppingsData.filterIsInstance<String>()
                            } else {
                                emptyList()
                            }

                            android.util.Log.d("ProductRepo", "Product data processed successfully")

                            // Return complete product
                            return product.copy(
                                sizesAvailable = sizes,
                                toppingsAvailable = toppings
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ProductRepo", "Error processing product data: ${e.message}", e)
                            null
                        }
                    }
                }
            }

            android.util.Log.e("ProductRepo", "Product not found with ID: $productId")
            null
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error fetching product with ID: $productId", e)
            null
        }
    }
}