package com.bh.beanie.repository

import android.content.Context
import android.util.Log
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductTopping
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.gson.Gson
import androidx.core.content.edit

class OrderRepository(private val db: FirebaseFirestore, private val context: Context) {
    private val cartPreferences = CartPreferences(context)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Lấy các mục trong giỏ hàng
    suspend fun getCartItems(): List<OrderItem> {
        return cartPreferences.getCart()
    }

    // Thêm sản phẩm vào giỏ hàng
    suspend fun addToCart(
        product: Product,
        selectedSize: Pair<String, Double>?,
        selectedToppings: List<ProductTopping>,
        quantity: Int,
        note: String
    ): Int {
        // Tạo OrderItem mới
        val newItem = OrderItem(
            productId = product.id,
            productName = product.name,
            categoryId = product.categoryId,
            size = selectedSize?.first ?: "",
            quantity = quantity,
            unitPrice = (selectedSize?.second ?: 0.0) + selectedToppings.sumOf { it.price },
            toppings = selectedToppings,
            note = note
        )

        return cartPreferences.addItem(newItem)
    }

    // Cập nhật số lượng một mục trong giỏ hàng
    suspend fun updateCartItemQuantity(position: Int, newQuantity: Int): Int {
        val currentCart = cartPreferences.getCart().toMutableList()
        if (position < 0 || position >= currentCart.size) return currentCart.sumOf { it.quantity }

        if (newQuantity <= 0) {
            return cartPreferences.removeItem(position)
        } else {
            val item = currentCart[position]
            val updatedItem = item.copy(quantity = newQuantity)
            return cartPreferences.updateItem(position, updatedItem)
        }
    }

    // Xóa mục khỏi giỏ hàng
    suspend fun removeFromCart(position: Int): Int {
        return cartPreferences.removeItem(position)
    }

    // Xóa toàn bộ giỏ hàng
    fun clearCart() {
        cartPreferences.clearCart()
    }

    // Tính tổng giá trị giỏ hàng
    suspend fun calculateTotal(): Double {
        return cartPreferences.calculateTotal()
    }

    // Cập nhật một mục trong giỏ hàng
    suspend fun updateCartItem(
        position: Int,
        product: Product,
        selectedSize: Pair<String, Double>?,
        selectedToppings: List<ProductTopping>,
        quantity: Int,
        note: String
    ): Int {
        Log.d("OrderRepo", "Updating cart item at position $position")

        // Tạo cart item mới
        val updatedItem = OrderItem(
            productId = product.id,
            productName = product.name,
            categoryId = product.categoryId,
            unitPrice = (selectedSize?.second ?: 0.0) + selectedToppings.sumOf { it.price },
            size = selectedSize?.first ?: "",
            toppings = selectedToppings.toMutableList(),
            quantity = quantity,
            note = note
        )

        return cartPreferences.updateItem(position, updatedItem)
    }

    // Tạo đơn hàng từ giỏ hàng
    suspend fun createOrder(order: Order): String {
        val cartItems = getCartItems()
        if (cartItems.isEmpty()) throw Exception("Giỏ hàng trống")
        val totalPrice = calculateTotal()

        // Tạo document order
        val orderRef = db.collection("orders").document()

        val orderData = mapOf(
            "branchId" to order.branchId,
            "userId" to order.userId,
            "customerName" to order.customerName,
            "phoneNumber" to order.phoneNumber,
            "deliveryAddress" to order.deliveryAddress,
            "type" to order.type,
            "totalPrice" to totalPrice,
            "status" to "WAITING ACCEPT",
            "orderTime" to order.orderTime,
            "paymentMethod" to order.paymentMethod,
            "note to" to order.note,
        )

        // Lưu order
        db.collection("orders").document(orderRef.id).set(orderData).await()

        // Lưu các order items
        cartItems.forEach { item ->
            db.collection("orders").document(orderRef.id)
                .collection("order_items").add(item).await()
        }

        // Xóa giỏ hàng sau khi đặt hàng thành công
        clearCart()

        return orderRef.id
    }

    // Lấy lịch sử đơn hàng của người dùng hiện tại
    suspend fun getUserOrders(): List<Order> {
        if (userId.isEmpty()) return emptyList()

        try {
            val snapshot = db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val order = doc.toObject(Order::class.java)
                    order?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("OrderRepository", "Error converting document to Order: ${e.message}")
                    null
                }
            }.sortedByDescending {
                try {
                    it.orderTime.seconds
                } catch (e: Exception) {
                    0
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "Error getting user orders: ${e.message}")
            return emptyList()
        }
    }

    // Lấy đơn hàng theo id
    suspend fun getOrderById(orderId: String): Order? {
        try {
            val doc = db.collection("orders").document(orderId).get().await()
            if (!doc.exists()) return null

            // Tạo đối tượng Order cơ bản (chưa có items)
            val order = doc.toObject(Order::class.java) ?: return null

            // Lấy các items của đơn hàng riêng biệt
            val items = fetchOrderItems(orderId)

            // Trả về order với items đã được xử lý
            return order.copy(id = doc.id, items = items)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error getting order: ${e.message}")
            throw e
        }
    }

    // Hàm fetchOrderItems xử lý items riêng biệt
    private suspend fun fetchOrderItems(orderId: String): List<OrderItem> {
        val itemsCollection = db.collection("orders").document(orderId).collection("items")
        val snapshot = itemsCollection.get().await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                // Lấy dữ liệu cơ bản
                val productId = doc.getString("productId") ?: ""
                val productName = doc.getString("productName") ?: ""
                val size = doc.getString("size") ?: ""
                val quantity = doc.getLong("quantity")?.toInt() ?: 0
                val unitPrice = doc.getDouble("unitPrice") ?: 0.0

                // Tạo OrderItem với dữ liệu đã xử lý
                OrderItem(
                    productId = productId,
                    productName = productName,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    size = size,
                )
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error parsing order item: ${e.message}")
                null
            }
        }
    }
}