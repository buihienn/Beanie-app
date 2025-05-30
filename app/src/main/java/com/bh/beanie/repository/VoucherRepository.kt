package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Voucher
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VoucherRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val vouchersCollection = firestore.collection("vouchers")

    suspend fun getAllVouchers(): List<Voucher> {
        return try {
            val snapshot = vouchersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                Voucher(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    content = doc.getString("content") ?: "",
                    expiryDate = doc.getTimestamp("expiryDate") ?: Timestamp.now(),
                    state = doc.getString("state") ?: "ACTIVE",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    discountType = doc.getString("discountType") ?: "PERCENTAGE",
                    discountValue = doc.getDouble("discountValue") ?: 0.0,
                    minOrderAmount = doc.getDouble("minOrderAmount") ?: 0.0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActiveVouchers(): List<Voucher> {
        return try {
            val currentTime = Timestamp.now()
            val snapshot = vouchersCollection
                .whereEqualTo("state", "ACTIVE")
                .whereGreaterThan("expiryDate", currentTime)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Voucher(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    content = doc.getString("content") ?: "",
                    expiryDate = doc.getTimestamp("expiryDate") ?: Timestamp.now(),
                    state = doc.getString("state") ?: "ACTIVE",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    discountType = doc.getString("discountType") ?: "PERCENTAGE",
                    discountValue = doc.getDouble("discountValue") ?: 0.0,
                    minOrderAmount = doc.getDouble("minOrderAmount") ?: 0.0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVoucherById(voucherId: String): Voucher? {
        return try {
            val document = vouchersCollection.document(voucherId).get().await()
            if (document.exists()) {
                Voucher(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    content = document.getString("content") ?: "",
                    expiryDate = document.getTimestamp("expiryDate") ?: Timestamp.now(),
                    state = document.getString("state") ?: "ACTIVE",
                    imageUrl = document.getString("imageUrl") ?: "",
                    discountType = document.getString("discountType") ?: "PERCENTAGE",
                    discountValue = document.getDouble("discountValue") ?: 0.0,
                    minOrderAmount = document.getDouble("minOrderAmount") ?: 0.0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun markVoucherAsUsed(userId: String, voucherId: String): Boolean {
        return try {
            val userVouchersCollection = firestore.collection("user_vouchers")

            // Tìm user_voucher dựa trên userId và voucherId
            val query = userVouchersCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("voucherId", voucherId)
                .limit(1)
                .get()
                .await()

            if (!query.isEmpty) {
                // Tìm thấy bản ghi, cập nhật trạng thái used thành true
                val userVoucherDoc = query.documents[0]
                userVoucherDoc.reference.update("used", true).await()
                Log.d("VoucherRepository", "Voucher marked as used: $voucherId for user: $userId")
                true
            } else {
                // Không tìm thấy bản ghi
                Log.w("VoucherRepository", "No user_voucher record found for userId: $userId, voucherId: $voucherId")
                false
            }
        } catch (e: Exception) {
            Log.e("VoucherRepository", "Error marking voucher as used", e)
            false
        }
    }

    companion object {
        @Volatile
        private var instance: VoucherRepository? = null

        fun getInstance(): VoucherRepository {
            return instance ?: synchronized(this) {
                instance ?: VoucherRepository().also { instance = it }
            }
        }
    }
}