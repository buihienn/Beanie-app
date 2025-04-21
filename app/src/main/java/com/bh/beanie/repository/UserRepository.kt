package com.bh.beanie.repository

import com.bh.beanie.model.User
import java.util.Calendar
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(private val db: FirebaseFirestore) {

    // Kiểm tra và reset điểm nếu cần khi người dùng đăng nhập
    fun checkAndResetPointsIfNeeded(userId: String) {
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java) ?: return@addOnSuccessListener

            // Lấy thời gian reset cuối cùng
            val lastReset = user.lastPointReset
            if (lastReset == null) {
                // Chưa bao giờ reset, kiểm tra xem đã qua tháng 6 năm nay chưa
                val now = Calendar.getInstance()
                val currentMonth = now.get(Calendar.MONTH)

                if (currentMonth >= Calendar.JUNE) {
                    // Đã qua tháng 6 năm nay, reset điểm
                    userRef.update(
                        mapOf(
                            "presentPoints" to 0,
                            "lastPointReset" to com.google.firebase.Timestamp.now()
                        )
                    )
                }
                return@addOnSuccessListener
            }

            // Convert timestamp to Calendar
            val lastResetCalendar = Calendar.getInstance()
            lastResetCalendar.time = lastReset

            val lastResetYear = lastResetCalendar.get(Calendar.YEAR)
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)

            // Nếu năm hiện tại lớn hơn năm reset cuối và tháng hiện tại >= tháng 6
            if (currentYear > lastResetYear && currentMonth >= Calendar.JUNE) {
                // Reset điểm
                userRef.update(
                    mapOf(
                        "presentPoints" to 0,
                        "lastPointReset" to com.google.firebase.Timestamp.now()
                    )
                )
            }
        }
    }
}