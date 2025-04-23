package com.bh.beanie.utils

import android.app.Activity
import android.content.Intent
import com.bh.beanie.BeanieApplication
import com.bh.beanie.admin.AdminMainActivity
import com.bh.beanie.customer.LoginActivity
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.user.UserMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object NavigationUtils {
    fun navigateBasedOnRole(activity: Activity, userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")

                when (role?.lowercase()) {
                    "admin" -> navigateToAdmin(activity)
                    else -> navigateToCustomer(activity, userId)
                }
            }
            .addOnFailureListener {
                navigateToLogin(activity)
            }
    }

    fun navigateToLogin(activity: Activity) {
        // Xóa dữ liệu người dùng
        BeanieApplication.instance.clearUserId()
        UserPreferences.clearUserData(activity)

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun navigateToAdmin(activity: Activity) {
        val intent = Intent(activity, AdminMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun navigateToCustomer(activity: Activity, userId: String) {
        // Kiểm tra và reset điểm nếu cần
        val userRepository = UserRepository()
        userRepository.checkAndResetPointsIfNeeded(userId)

        val intent = Intent(activity, UserMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun logout(activity: Activity) {
        // Đăng xuất khỏi Firebase Auth
        FirebaseAuth.getInstance().signOut()

        // Chuyển đến màn hình đăng nhập
        navigateToLogin(activity)
    }
}