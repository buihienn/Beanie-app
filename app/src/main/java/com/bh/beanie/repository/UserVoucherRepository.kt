package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.UserVoucher
import com.bh.beanie.model.Voucher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

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