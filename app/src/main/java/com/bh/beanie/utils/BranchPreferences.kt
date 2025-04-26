package com.bh.beanie.utils

import android.content.Context
import androidx.core.content.edit

object BranchPreferences {
    private const val PREF_NAME = "BeanieBranchPrefs"
    private const val KEY_BRANCH_ID = "branch_id"
    private const val KEY_BRANCH_NAME = "branch_name"
    private const val KEY_BRANCH_ADDRESS = "branch_address"

    /**
     * Lưu ID chi nhánh vào SharedPreferences
     */
    fun saveBranchId(context: Context, branchId: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit { putString(KEY_BRANCH_ID, branchId) }
    }

    /**
     * Lấy ID chi nhánh từ SharedPreferences
     * @return Chuỗi rỗng nếu không tìm thấy
     */
    fun getBranchId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_BRANCH_ID, "") ?: ""
    }

    /**
     * Lưu tên chi nhánh
     */
    fun saveBranchName(context: Context, name: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit { putString(KEY_BRANCH_NAME, name) }
    }

    /**
     * Lấy tên chi nhánh
     */
    fun getBranchName(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_BRANCH_NAME, "") ?: ""
    }

    /**
     * Lưu địa chỉ chi nhánh
     */
    fun saveBranchAddress(context: Context, address: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit { putString(KEY_BRANCH_ADDRESS, address) }
    }

    /**
     * Lấy địa chỉ chi nhánh
     */
    fun getBranchAddress(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_BRANCH_ADDRESS, "") ?: ""
    }

    /**
     * Lưu toàn bộ thông tin chi nhánh
     */
    fun saveBranch(context: Context, branchId: String, name: String, address: String, imageUrl: String = "") {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit {
            putString(KEY_BRANCH_ID, branchId)
            putString(KEY_BRANCH_NAME, name)
            putString(KEY_BRANCH_ADDRESS, address)
        }
    }

    /**
     * Xóa toàn bộ thông tin chi nhánh
     */
    fun clearBranch(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit { clear() }
    }

    /**
     * Kiểm tra chi nhánh đã được chọn chưa
     */
    fun hasBranchSelected(context: Context): Boolean {
        return getBranchId(context).isNotEmpty()
    }
}