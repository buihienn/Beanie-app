package com.bh.beanie.repository

import android.content.Context
import android.util.Log
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductTopping
import com.bh.beanie.model.Voucher
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.String
import kotlin.text.get
import kotlin.text.toInt

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
            "transactionId" to order.transactionId,
            "note" to order.note,
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
            val orderRef = db.collection("orders").document(orderId)
            val snapshot = orderRef.get().await()

            if (!snapshot.exists()) {
                Log.d("OrderRepository", "Order không tồn tại với ID: $orderId")
                return null
            }

            // Lấy thông tin cơ bản của order
            val order = Order(
                id = snapshot.id,
                branchId = snapshot.getString("branchId") ?: "",
                userId = snapshot.getString("userId") ?: "",
                customerName = snapshot.getString("customerName") ?: "",
                phoneNumber = snapshot.getString("phoneNumber") ?: "",
                deliveryAddress = snapshot.getString("deliveryAddress") ?: "",
                type = snapshot.getString("type") ?: "DELIVERY",
                totalPrice = snapshot.getDouble("totalPrice") ?: 0.0,
                status = snapshot.getString("status") ?: "WAITING ACCEPT",
                orderTime = snapshot.getTimestamp("orderTime") ?: Timestamp.now(),
                note = snapshot.getString("note") ?: "",
                paymentMethod = snapshot.getString("paymentMethod") ?: "CASH",
                transactionId = snapshot.getString("transactionId") ?: ""
            )

            // Log để kiểm tra thông tin order cơ bản
            Log.d("OrderRepository", "Đã lấy order cơ bản: ${order.id}, status: ${order.status}" +
                    "branchId: ${order.branchId}, userId: ${order.userId}, " +
                    "deliveryAddress: ${order.deliveryAddress}, type: ${order.type}, " +
                    "totalPrice: ${order.totalPrice}, status: ${order.status}, ")

            // Lấy các items của đơn hàng từ subcollection
            val items = fetchOrderItems(orderId)
            Log.d("OrderRepository", "Đã lấy ${items.size} items cho order $orderId")

            // Thêm log để kiểm tra items
            items.forEachIndexed { index, item ->
                Log.d("OrderRepository", "Item $index: ${item.productName}, qty: ${item.quantity}")
            }

            // Trả về order với items đã được xử lý
            return order.copy(items = items)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Lỗi khi lấy order: ${e.message}")
            throw e
        }
    }

    // Hàm fetchOrderItems xử lý items riêng biệt
    private suspend fun fetchOrderItems(orderId: String): List<OrderItem> {
        val itemsCollection = db.collection("orders").document(orderId).collection("order_items")
        val snapshot = itemsCollection.get().await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                // Lấy dữ liệu cơ bản
                val productId = doc.getString("productId") ?: ""
                val productName = doc.getString("productName") ?: ""
                val categoryId = doc.getString("categoryId") ?: ""
                val size = doc.getString("size") ?: ""
                val quantity = doc.getLong("quantity")?.toInt() ?: 0
                val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                val note = doc.getString("note") ?: ""

                // Xử lý danh sách toppings
                val toppingsData = doc.get("toppings") as? List<Map<String, Any>> ?: emptyList()
                val toppings = toppingsData.map { toppingMap ->
                    ProductTopping(
                        id = toppingMap["id"] as? String ?: "",
                        name = toppingMap["name"] as? String ?: "",
                        price = (toppingMap["price"] as? Number)?.toDouble() ?: 0.0
                    )
                }

                // Tạo OrderItem với dữ liệu đã xử lý
                OrderItem(
                    productId = productId,
                    productName = productName,
                    categoryId = categoryId,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    size = size,
                    note = note,
                    toppings = toppings
                )
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error parsing order item: ${e.message}")
                null
            }
        }
    }
}