package com.bh.beanie.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.bh.beanie.BeanieApplication
import com.bh.beanie.admin.AdminMainActivity
import com.bh.beanie.user.LoginActivity
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
                val name: String = document.getString("username") ?: ""

                when (role?.lowercase()) {
                    "admin" -> {
                        // Query the branch managed by this admin
                        db.collection("branches")
                            .whereEqualTo("manageID", userId)
                            .get()
                            .addOnSuccessListener { branchSnapshot ->
                                if (!branchSnapshot.isEmpty) {
                                    val branchId = branchSnapshot.documents.first().id
                                    navigateToAdmin(activity, branchId, name) // Pass Branch ID
                                } else {
                                    // Handle case where no branch is found
                                    navigateToLogin(activity)
                                }
                            }
                            .addOnFailureListener {
                                navigateToLogin(activity)
                            }
                    }
                    else -> navigateToCustomer(activity, userId)
                }
            }
            .addOnFailureListener {
                navigateToLogin(activity)
            }
    }

    fun navigateToLogin(activity: Activity) {
        // Xóa dữ liệu người dùng
        Log.d("DEBUG", "Navigating to LoginActivity")
        BeanieApplication.instance.clearUserId()
        UserPreferences.clearUserData(activity)

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun navigateToAdmin(activity: Activity, branchId: String, name: String) {
        Log.d("DEBUG", "Navigating to AdminMainActivity with branchId: $branchId")
        val intent = Intent(activity, AdminMainActivity::class.java)
        intent.putExtra("branchId", branchId)
        intent.putExtra("nameAdmin", name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun navigateToCustomer(activity: Activity, userId: String) {
        Log.d("DEBUG", "Navigating to UserMainActivity with userId: $userId")
        val userRepository = UserRepository()
        userRepository.checkAndResetPointsIfNeeded(userId)

        val intent = Intent(activity, UserMainActivity::class.java)
        // Add the userId to the intent
        intent.putExtra("USER_ID", userId)

        // Optionally add email and name if available
        val auth = FirebaseAuth.getInstance()
        intent.putExtra("USER_EMAIL", auth.currentUser?.email ?: "")
        intent.putExtra("USER_NAME", auth.currentUser?.displayName ?: "")

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