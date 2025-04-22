package com.bh.beanie.repository

import android.content.Context
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductSize
import com.bh.beanie.model.ProductTopping
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class OrderRepository(private val db: FirebaseFirestore, private val context: Context) {
    private val firebaseRepository = FirebaseRepository(db)
    private val sharedPreferences = context.getSharedPreferences("CartPreferences", Context.MODE_PRIVATE)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Lưu giỏ hàng vào SharedPreferences dưới dạng JSON
    private suspend fun saveCartToPrefs(cartItems: List<OrderItem>) {
        val gson = Gson()
        val cartJson = gson.toJson(cartItems)
        sharedPreferences.edit().putString("CART_$userId", cartJson).apply()
    }

    // Đọc giỏ hàng từ SharedPreferences
    suspend fun getCartItems(): List<OrderItem> {
        val gson = Gson()
        val cartJson = sharedPreferences.getString("CART_$userId", null)
        if (cartJson.isNullOrEmpty()) return emptyList()

        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                List::class.java, OrderItem::class.java
            ).type
            gson.fromJson(cartJson, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Thêm sản phẩm vào giỏ hàng
    suspend fun addToCart(
        product: Product,
        selectedSize: ProductSize?,
        selectedToppings: List<ProductTopping>,
        quantity: Int,
        note: String
    ): Int {
        val currentCart = getCartItems().toMutableList()

        // Tạo một OrderItem mới
        val newItem = OrderItem(
            productId = product.id,
            productName = product.name,
            size = selectedSize,
            quantity = quantity,
            unitPrice = (selectedSize?.price ?: product.price) + selectedToppings.sumOf { it.price },
            toppings = selectedToppings,
            note = note
        )

        // Thêm vào giỏ hàng
        currentCart.add(newItem)

        // Lưu giỏ hàng mới
        saveCartToPrefs(currentCart)

        // Trả về tổng số lượng sản phẩm trong giỏ hàng
        return currentCart.sumOf { it.quantity }
    }

    // Cập nhật số lượng của một sản phẩm trong giỏ hàng
    suspend fun updateCartItemQuantity(position: Int, newQuantity: Int): Int {
        val currentCart = getCartItems().toMutableList()
        if (position < 0 || position >= currentCart.size) return currentCart.sumOf { it.quantity }

        if (newQuantity <= 0) {
            // Xóa khỏi giỏ hàng nếu số lượng <= 0
            currentCart.removeAt(position)
        } else {
            // Cập nhật số lượng
            val item = currentCart[position]
            currentCart[position] = item.copy(quantity = newQuantity)
        }

        saveCartToPrefs(currentCart)
        return currentCart.sumOf { it.quantity }
    }

    // Xóa một sản phẩm khỏi giỏ hàng
    suspend fun removeFromCart(position: Int): Int {
        val currentCart = getCartItems().toMutableList()
        if (position >= 0 && position < currentCart.size) {
            currentCart.removeAt(position)
            saveCartToPrefs(currentCart)
        }
        return currentCart.sumOf { it.quantity }
    }

    // Xóa toàn bộ giỏ hàng
    suspend fun clearCart() {
        saveCartToPrefs(emptyList())
    }

    // Tính tổng giá trị đơn hàng
    suspend fun calculateTotal(): Double {
        val cartItems = getCartItems()
        return cartItems.sumOf { it.unitPrice * it.quantity }
    }

    // Tạo đơn hàng từ giỏ hàng
    suspend fun createOrder(
        branchId: String,
        customerName: String,
        phoneNumber: String,
        deliveryAddress: String,
        type: String,
        paymentMethod: String,
        note: String
    ): String {
        val cartItems = getCartItems()
        if (cartItems.isEmpty()) throw Exception("Giỏ hàng trống")

        val totalPrice = calculateTotal()

        // Tạo document order
        val orderRef = db.collection("orders").document()
        val order = Order(
            id = orderRef.id,
            branchId = branchId,
            userId = userId,
            customerName = customerName,
            phoneNumber = phoneNumber,
            deliveryAddress = deliveryAddress,
            type = type,
            items = cartItems,
            totalPrice = totalPrice,
            paymentMethod = paymentMethod,
            note = note
        )

        // Lưu order
        db.collection("orders").document(orderRef.id).set(order).await()

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

        val snapshot = db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("orderTime")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val order = doc.toObject(Order::class.java)
            order?.copy(id = doc.id)
        }
    }

    // Lấy đơn hàng theo id
    suspend fun getOrderById(orderId: String): Order? {
        val doc = db.collection("orders").document(orderId).get().await()
        val order = doc.toObject(Order::class.java) ?: return null

        // Lấy các items của đơn hàng
        val items = fetchOrderItems(orderId)
        return order.copy(id = doc.id, items = items)
    }

    // Lấy danh sách sản phẩm trong đơn hàng
    suspend fun fetchOrderItems(orderId: String): List<OrderItem> {
        val itemsSnapshot = db.collection("orders")
            .document(orderId)
            .collection("order_items")
            .get()
            .await()

        return itemsSnapshot.map { doc ->
            // Tạo một đối tượng ProductSize từ thông tin trong document
            val sizeName = doc.getString("size") ?: ""
            val sizePrice = doc.getDouble("sizePrice") ?: 0.0

            // Nếu không có thông tin size, đặt thành null
            val productSize = if (sizeName.isNotEmpty()) {
                ProductSize(name = sizeName, price = sizePrice)
            } else {
                null
            }

            OrderItem(
                productId = doc.getString("productId") ?: "",
                productName = doc.getString("productName") ?: "",
                size = productSize,
                quantity = doc.getLong("quantity")?.toInt() ?: 0,
                unitPrice = doc.getDouble("unitPrice") ?: 0.0
            )
        }
    }

    suspend fun updateCartItem(
        position: Int,
        product: Product,
        selectedSize: ProductSize?,
        selectedToppings: List<ProductTopping>,
        quantity: Int,
        note: String
    ): Int {
        android.util.Log.d("OrderRepo", "Updating cart item at position $position")
        android.util.Log.d("OrderRepo", "Product: ${product.id}, ${product.name}")
        android.util.Log.d("OrderRepo", "Size: ${selectedSize?.name}, toppings: ${selectedToppings.size}, qty: $quantity")

        val cartItems = getCartItems().toMutableList()

        if (position < 0 || position >= cartItems.size) {
            android.util.Log.e("OrderRepo", "Invalid position: $position, cart size: ${cartItems.size}")
            return cartItems.size
        }

        // Tạo cart item mới
        val updatedItem = OrderItem(
            productId = product.id,
            productName = product.name,
            unitPrice = product.price,
            size = selectedSize,
            toppings = selectedToppings.toMutableList(),
            quantity = quantity,
            note = note
        )

        // Cập nhật item tại vị trí position
        cartItems[position] = updatedItem

        // Lưu danh sách mới vào shared preferences
        saveCartItems(cartItems)

        android.util.Log.d("OrderRepo", "Cart updated. New size: ${cartItems.size}")
        return cartItems.size
    }

    private fun saveCartItems(items: List<OrderItem>) {
        val gson = Gson()
        val json = gson.toJson(items)

        sharedPreferences.edit() { putString("CART_$userId", json) }

        android.util.Log.d("OrderRepo", "Saved ${items.size} items to cart")
    }

    // Tính tổng giá của một mục sản phẩm
    private fun calculateItemTotalPrice(
        basePrice: Double,
        size: ProductSize?,
        toppings: List<ProductTopping>,
        quantity: Int
    ): Double {
        // Giá cơ bản là giá theo size hoặc giá gốc nếu không có size
        val sizePrice = size?.price ?: basePrice

        // Tổng giá topping
        val toppingPrice = toppings.sumOf { it.price }

        // Tổng giá = (giá size + giá topping) * số lượng
        return (sizePrice + toppingPrice) * quantity
    }
}