package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Category
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.model.Product
import com.bh.beanie.model.User
import com.bh.beanie.model.Voucher
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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

    suspend fun fetchCategoryItemsSuspend(branchId: String, categoryId: String): List<Product> {
        val productsRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId).collection("products")
        val snapshot = productsRef.get().await()

        return snapshot.map { doc ->
            Product(
                id = doc.id,
                name = doc.getString("name") ?: "Unnamed Product",
                description = doc.getString("description") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                imageUrl = doc.getString("imageUrl") ?: "",
                stockQuantity = doc.getLong("stock")?.toInt() ?: 0,
                categoryId = categoryId,
                size = doc.get("size") as? Map<String, Double> ?: emptyMap(),
            )
        }
    }

    suspend fun addCategorySuspend(branchId: String, category: Category) {
        val categoriesRef = db.collection("branches").document(branchId).collection("categories")
        val categoryData = mapOf("name" to category.name)
        categoriesRef.document(category.id).set(categoryData).await()
    }

    suspend fun addCategoryItemSuspend(branchId: String, categoryId: String, item: Product) {
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
            "stock" to item.stockQuantity,
            "size" to item.size
        )

        productsRef.set(itemData).await()
    }

    suspend fun editCategoryItemSuspend(branchId: String, categoryId: String, item: Product) {
        val itemRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(item.id)

        val updatedData = mapOf(
            "name" to item.name,
            "description" to item.description,
            "price" to item.price,
            "imageUrl" to item.imageUrl,
            "stock" to item.stockQuantity,
            "size" to item.size
        )

        itemRef.update(updatedData).await()
    }

    suspend fun deleteCategoryItemSuspend(branchId: String, categoryId: String, itemId: String) {
        val itemRef = db.collection("branches").document(branchId)
            .collection("categories").document(categoryId)
            .collection("products").document(itemId)

        itemRef.delete().await()
    }


    // voucher

    suspend fun addVoucherSuspend(voucher: Voucher) {
        val voucherRef = db.collection("vouchers").document(voucher.id.ifEmpty { db.collection("vouchers").document().id })

        val voucherData = mapOf(
            "name" to voucher.name,
            "content" to voucher.content,
            "expiryDate" to voucher.expiryDate,
            "state" to voucher.state,
            "imageUrl" to voucher.imageUrl,
            "discountType" to voucher.discountType,
            "discountValue" to voucher.discountValue,
            "minOrderAmount" to voucher.minOrderAmount
        )

        voucherRef.set(voucherData).await()
    }

    suspend fun fetchVouchersSuspend(): List<Voucher> {
        val vouchersRef = db.collection("vouchers")
        val snapshot = vouchersRef.get().await()

        return snapshot.map { doc ->
            Voucher(
                id = doc.id,
                name = doc.getString("name") ?: "",
                content = doc.getString("content") ?: "",
                expiryDate = doc.getTimestamp("expiryDate") ?: Timestamp.now(),
                state = doc.getString("state") ?: "ACTIVE",
                imageUrl = doc.getString("imageUrl") ?: "",
                discountType = doc.getString("discountType") ?: "PERCENT",
                discountValue = doc.getDouble("discountValue") ?: 0.0,
                minOrderAmount = doc.getDouble("minOrderAmount") ?: 0.0
            )
        }
    }

    // Lấy danh sách sản phẩm trong đơn hàng
    private suspend fun fetchOrderItems(orderId: String): List<OrderItem> {
        val itemsSnapshot = db.collection("orders")
            .document(orderId)
            .collection("order_items")
            .get()
            .await()

        return itemsSnapshot.map { doc ->

            OrderItem(
                productId = doc.getString("productId") ?: "",
                productName = doc.getString("productName") ?: "",
                size = doc.getString("size") ?: "",
                quantity = doc.getLong("quantity")?.toInt() ?: 0,
                unitPrice = doc.getDouble("unitPrice") ?: 0.0
            )
        }
    }

    // Fetch danh sách đơn hàng, thêm filter branchId
    suspend fun fetchOrdersPaginated(
        branchId: String?, // <- Thêm branchId vô đây
        lastVisibleDocument: DocumentSnapshot? = null
    ): Pair<List<Order>, DocumentSnapshot?> = coroutineScope {
        var query: Query = db.collection("orders")
            .orderBy("orderTime", Query.Direction.DESCENDING)
            .limit(7)

        if (!branchId.isNullOrEmpty()) {
            query = query.whereEqualTo("branchId", branchId)
        }

        lastVisibleDocument?.let {
            query = query.startAfter(it)
        }

        val ordersSnapshot = query.get().await()

        val orders = ordersSnapshot.map { doc ->
            async {
                val orderId = doc.id
                val order = Order(
                    id = orderId,
                    branchId = doc.getString("branchId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    customerName = doc.getString("customerName") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    deliveryAddress = doc.getString("deliveryAddress") ?: "",
                    type = doc.getString("type") ?: "DELIVERY",
                    totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                    status = doc.getString("status") ?: "WAITING ACCEPT",
                    orderTime = doc.getTimestamp("orderTime") ?: Timestamp.now(),
                    paymentMethod = doc.getString("paymentMethod") ?: "CASH",
                    note = doc.getString("note") ?: ""
                )
                val items = fetchOrderItems(orderId)
                order.copy(items = items)
            }
        }

        val orderList = orders.awaitAll()
        val lastDoc = ordersSnapshot.documents.lastOrNull()

        Pair(orderList, lastDoc)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        val orderRef = db.collection("orders").document(orderId)
        orderRef.update("status", newStatus).await()
    }

    suspend fun fetchCustomersPaginated(lastVisibleDocument: DocumentSnapshot? = null): Pair<List<User>, DocumentSnapshot?> {
        return try {
            var query = db.collection("users")
                .whereEqualTo("role", "customer")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)

            // Nếu có document cuối cùng từ lần trước, thì phân trang tiếp theo
            if (lastVisibleDocument != null) {
                query = query.startAfter(lastVisibleDocument)
            }

            val snapshot = query.get().await()

            val customers = snapshot.map { doc ->
                User(
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    dob = doc.getString("dob") ?: "",
                    gender = doc.getString("gender") ?: "",
                    avatarUrl = doc.getString("avatarUrl"),
                    role = doc.getString("role") ?: "customer",
                    createdAt = doc.getDate("createdAt")
                )
            }

            val lastDoc = snapshot.documents.lastOrNull()
            Pair(customers, lastDoc)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching paginated customers: ${e.message}")
            Pair(emptyList(), null)
        }
    }

    suspend fun updateCustomer(user: User) {
        try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", user.email)
                .get()
                .await()
            if (querySnapshot.isEmpty) {
                throw Exception("User with email ${user.email} not found.")
            }

            val userRef = querySnapshot.documents.first().reference

            val updatedData = mapOf(
                "username" to user.username,
                "email" to user.email,
                "phone" to user.phone,
                "dob" to user.dob,
                "gender" to user.gender,
                "avatarUrl" to user.avatarUrl,
                "role" to user.role,
                "createdAt" to user.createdAt
            )
            userRef.update(updatedData).await()
            Log.d("FirebaseRepository", "Customer updated successfully: ${user.email}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating customer: ${e.message}")
            throw e
        }
    }

}