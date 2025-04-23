package com.bh.beanie.repository

import com.bh.beanie.model.User
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class PointRepository(private val db: FirebaseFirestore) {

    // Thêm điểm khi người dùng hoàn thành đơn hàng
    fun addPoints(userId: String, orderAmount: Double, orderId: String) {
        val pointsToAdd = calculatePoints(orderAmount) // Ví dụ: 1 điểm cho mỗi 10,000đ

        db.runTransaction { transaction ->
            val userRef = db.collection("users").document(userId)
            val userSnapshot = transaction.get(userRef)
            val user = userSnapshot.toObject(User::class.java) ?: throw Exception("User not found")

            // Nếu đã đạt hạng cao nhất, chỉ cộng vào presentPoints
            val newPoints = if (user.points >= 1500) user.points else user.points + pointsToAdd
            val newPresentPoints = user.presentPoints + pointsToAdd
            val newLevel = calculateMembershipLevel(newPoints)

            // Cập nhật điểm và hạng thành viên
            transaction.update(userRef,
                mapOf(
                    "points" to newPoints,
                    "presentPoints" to newPresentPoints,
                    "membershipLevel" to newLevel
                )
            )

            // Lưu giao dịch điểm vào collection riêng
            val transactionRef = db.collection("pointTransactions").document()
            val pointTransaction = hashMapOf(
                "id" to transactionRef.id,
                "userId" to userId,
                "amount" to pointsToAdd,
                "type" to "ORDER_COMPLETED",
                "description" to "Tích điểm từ đơn hàng",
                "referenceId" to orderId,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            transaction.set(transactionRef, pointTransaction)
        }
    }

    // Sử dụng điểm để đổi phần thưởng
    fun usePoints(userId: String, pointsToUse: Int, rewardId: String) {
        if (pointsToUse <= 0) throw Exception("Số điểm phải lớn hơn 0")

        db.runTransaction { transaction ->
            val userRef = db.collection("users").document(userId)
            val userSnapshot = transaction.get(userRef)
            val user = userSnapshot.toObject(User::class.java) ?: throw Exception("User not found")

            if (user.presentPoints < pointsToUse) {
                throw Exception("Không đủ điểm để đổi phần thưởng")
            }

            val newPresentPoints = user.presentPoints - pointsToUse

            // Chỉ cập nhật presentPoints, không thay đổi points và hạng thành viên
            transaction.update(userRef, "presentPoints", newPresentPoints)

            // Lưu giao dịch điểm vào collection riêng
            val transactionRef = db.collection("pointTransactions").document()
            val pointTransaction = hashMapOf(
                "id" to transactionRef.id,
                "userId" to userId,
                "amount" to -pointsToUse,
                "type" to "REWARD_REDEMPTION",
                "description" to "Đổi phần thưởng",
                "referenceId" to rewardId,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            transaction.set(transactionRef, pointTransaction)
        }
    }

    // Tính số điểm từ giá trị đơn hàng
    private fun calculatePoints(orderAmount: Double): Int {
        // Ví dụ: 1 điểm cho mỗi 10,000đ
        return (orderAmount / 10000).toInt()
    }

    // Xác định hạng thành viên dựa trên tổng điểm
    private fun calculateMembershipLevel(points: Int): String {
        return when {
            points < 500 -> "Thành viên mới"
            points < 1500 -> "Thành viên thân thiết"
            else -> "Thành viên VIP"
        }
    }
}