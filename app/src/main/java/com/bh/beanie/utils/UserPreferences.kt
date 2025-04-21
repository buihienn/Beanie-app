package com.bh.beanie.utils

import android.content.Context
import androidx.core.content.edit

object UserPreferences {
    private const val PREF_NAME = "BeanieUserPrefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"

    /**
     * Lưu ID người dùng vào SharedPreferences
     */
    fun saveUserId(context: Context, userId: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { putString(KEY_USER_ID, userId) }
    }

    /**
     * Lấy ID người dùng từ SharedPreferences
     * @return Chuỗi rỗng nếu không tìm thấy
     */
    fun getUserId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_USER_ID, "") ?: ""
    }

    /**
     * Lưu vai trò người dùng
     */
    fun saveUserRole(context: Context, role: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { putString(KEY_USER_ROLE, role) }
    }

    /**
     * Lấy vai trò người dùng
     */
    fun getUserRole(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_USER_ROLE, "") ?: ""
    }

    /**
     * Lưu email người dùng
     */
    fun saveUserEmail(context: Context, email: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { putString(KEY_USER_EMAIL, email) }
    }

    /**
     * Lấy email người dùng
     */
    fun getUserEmail(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    /**
     * Lưu tên người dùng
     */
    fun saveUserName(context: Context, name: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { putString(KEY_USER_NAME, name) }
    }

    /**
     * Lấy tên người dùng
     */
    fun getUserName(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_USER_NAME, "") ?: ""
    }

    /**
     * Xóa thông tin người dùng khi đăng xuất
     */
    fun clearUserData(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { clear() }
    }

    /**
     * Xóa chỉ ID người dùng
     */
    fun clearUserId(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit() { remove(KEY_USER_ID) }
    }

    /**
     * Kiểm tra người dùng đã đăng nhập hay chưa
     */
    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context).isNotEmpty()
    }
}