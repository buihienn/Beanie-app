package com.bh.beanie.repository

import android.content.Context
import android.util.Log
import com.bh.beanie.model.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class CartPreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("CartPreferences", Context.MODE_PRIVATE)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val gson = Gson()

    // Lưu giỏ hàng vào SharedPreferences
    fun saveCart(cartItems: List<OrderItem>) {
        try {
            val cartJson = gson.toJson(cartItems)
            sharedPreferences.edit() { putString("CART_$userId", cartJson) }
            Log.d(TAG, "Đã lưu ${cartItems.size} sản phẩm vào giỏ hàng")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu giỏ hàng: ${e.message}", e)
        }
    }

    // Lấy giỏ hàng từ SharedPreferences
    fun getCart(): List<OrderItem> {
        val cartJson = sharedPreferences.getString("CART_$userId", null)
        if (cartJson.isNullOrEmpty()) return emptyList()

        return try {
            val type = TypeToken.getParameterized(List::class.java, OrderItem::class.java).type
            val cart = gson.fromJson<List<OrderItem>>(cartJson, type)
            Log.d(TAG, "Đã lấy ${cart.size} sản phẩm từ giỏ hàng")
            cart
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đọc giỏ hàng: ${e.message}", e)
            emptyList()
        }
    }

    // Xóa giỏ hàng
    fun clearCart() {
        sharedPreferences.edit() { remove("CART_$userId") }
        Log.d(TAG, "Đã xóa giỏ hàng")
    }

    // Thêm một mục vào giỏ hàng
    fun addItem(item: OrderItem): Int {
        val currentCart = getCart().toMutableList()
        currentCart.add(item)
        saveCart(currentCart)
        return currentCart.sumOf { it.quantity }
    }

    // Cập nhật một mục trong giỏ hàng
    fun updateItem(position: Int, item: OrderItem): Int {
        val currentCart = getCart().toMutableList()
        if (position < 0 || position >= currentCart.size) {
            Log.e(TAG, "Vị trí không hợp lệ: $position, kích thước giỏ hàng: ${currentCart.size}")
            return currentCart.sumOf { it.quantity }
        }

        currentCart[position] = item
        saveCart(currentCart)
        return currentCart.sumOf { it.quantity }
    }

    // Xóa một mục khỏi giỏ hàng
    fun removeItem(position: Int): Int {
        val currentCart = getCart().toMutableList()
        if (position >= 0 && position < currentCart.size) {
            currentCart.removeAt(position)
            saveCart(currentCart)
        }
        return currentCart.sumOf { it.quantity }
    }

    // Tính tổng giá trị giỏ hàng
    fun calculateTotal(): Double {
        val cartItems = getCart()
        return cartItems.sumOf { it.unitPrice * it.quantity }
    }

    companion object {
        private const val TAG = "CartPreferences"
    }
}