package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.UserVoucher
import com.bh.beanie.model.Voucher
import com.bh.beanie.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.text.set
import kotlin.toString

class UserVoucherRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userVouchersCollection = firestore.collection("user_vouchers")
    private val vouchersCollection = firestore.collection("vouchers")

    suspend fun getUserVouchers(): List<Pair<UserVoucher, Voucher>> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        return try {
            Log.d("UserVoucherRepository", "Fetching user vouchers for user ID: $currentUserId")

            val userVouchersSnapshot = userVouchersCollection
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("used", false) // Chỉ lấy các voucher chưa sử dụng
                .get()
                .await()

            Log.d("UserVoucherRepository", "Query returned ${userVouchersSnapshot.size()} documents")

            val userVouchers = userVouchersSnapshot.toObjects(UserVoucher::class.java)

            if (userVouchers.isEmpty()) {
                Log.d("UserVoucherRepository", "No user vouchers found for user $currentUserId")
                return emptyList()
            }

            val result = mutableListOf<Pair<UserVoucher, Voucher>>()

            for (userVoucher in userVouchers) {
                Log.d("UserVoucherRepository", "Trying to fetch voucher with ID: ${userVoucher.voucherId}")
                if (userVoucher.voucherId.isNotEmpty()) {
                    try {
                        val voucherDoc =
                            vouchersCollection.document(userVoucher.voucherId).get().await()
                        if (voucherDoc.exists()) {
                            val voucher = voucherDoc.toObject(Voucher::class.java)
                            if (voucher != null) {
                                voucher.id = voucherDoc.id
                                result.add(Pair(userVoucher, voucher))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("UserVoucherRepository", "Error fetching voucher: ${e.message}")
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e("UserVoucherRepository", "Error getting user vouchers", e)
            Log.e("UserVoucherRepository", "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    suspend fun assignVoucherToUser(voucher: Voucher): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        return try {
            val userVoucher = UserVoucher(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                voucherId = voucher.id,
                used = false
            )

            userVouchersCollection.document(userVoucher.id).set(userVoucher).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markVoucherAsUsed(userVoucherId: String): Boolean {
        return try {
            val now = com.google.firebase.Timestamp.now()
            userVouchersCollection.document(userVoucherId)
                .update(
                    mapOf(
                        "used" to true,
                        "usedDate" to now
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getActiveVouchers(): List<Voucher> {
        return try {
            val currentTime = com.google.firebase.Timestamp.now()
            val snapshot = vouchersCollection
                .whereEqualTo("state", "ACTIVE")
                .whereGreaterThan("expiryDate", currentTime)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Voucher::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun redeemVoucherWithPoints(voucher: Voucher): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        try {
            // Thực hiện giao dịch để đảm bảo tính toàn vẹn dữ liệu
            return firestore.runTransaction { transaction ->
                // 1. Lấy thông tin user hiện tại
                val userRef = firestore.collection("users").document(currentUserId)
                val userSnapshot = transaction.get(userRef)
                val user = userSnapshot.toObject(User::class.java) ?: throw Exception("User not found")

                // 2. Kiểm tra người dùng có đủ điểm không
                if (user.presentPoints < voucher.redeemPoints) {
                    throw Exception("Không đủ điểm để đổi voucher này")
                }

                // 3. Tạo user_voucher mới
                val userVoucher = UserVoucher(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    voucherId = voucher.id,
                    used = false
                )

                // 4. Trừ điểm người dùng
                val newPoints = user.presentPoints - voucher.redeemPoints

                // 5. Cập nhật điểm người dùng
                transaction.update(userRef, "presentPoints", newPoints)

                // 6. Lưu user_voucher mới
                val userVoucherRef = userVouchersCollection.document(userVoucher.id)
                transaction.set(userVoucherRef, userVoucher)

                true
            }.await()
        } catch (e: Exception) {
            Log.e("UserVoucherRepository", "Error redeeming voucher: ${e.message}")
            throw e
        }
    }

    suspend fun getActiveVouchersForRedeem(): List<Voucher> {
        return try {
            val currentTime = com.google.firebase.Timestamp.now()
            // Chỉ sử dụng 2 điều kiện lọc
            val snapshot = vouchersCollection
                .whereEqualTo("state", "ACTIVE")
                .whereGreaterThan("expiryDate", currentTime)
                .get()
                .await()

            val vouchers = mutableListOf<Voucher>()
            for (doc in snapshot.documents) {
                val voucher = doc.toObject(Voucher::class.java)
                if (voucher != null) {
                    voucher.id = doc.id
                    // Lọc thêm điều kiện trong ứng dụng
                    if (voucher.redeemPoints > 0) {
                        vouchers.add(voucher)
                    }
                }
            }
            vouchers
        } catch (e: Exception) {
            Log.e("UserVoucherRepository", "Error getting vouchers for redeem: ${e.message}")
            emptyList()
        }
    }

    companion object {
        @Volatile
        private var instance: UserVoucherRepository? = null

        fun getInstance(): UserVoucherRepository {
            return instance ?: synchronized(this) {
                instance ?: UserVoucherRepository().also { instance = it }
            }
        }
    }
}