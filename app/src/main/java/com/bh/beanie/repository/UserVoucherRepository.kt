package com.bh.beanie.repository

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
            val userVouchersSnapshot = userVouchersCollection
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isUsed", false)
                .get()
                .await()

            val userVouchers = userVouchersSnapshot.documents.mapNotNull {
                it.toObject(UserVoucher::class.java)
            }

            // Get full voucher details for each user voucher
            val voucherDetailsMap = mutableMapOf<String, Voucher>()

            for (userVoucher in userVouchers) {
                if (voucherDetailsMap.containsKey(userVoucher.voucherId)) continue

                val voucherSnapshot = vouchersCollection.document(userVoucher.voucherId).get().await()
                val voucher = voucherSnapshot.toObject(Voucher::class.java)

                voucher?.let { voucherDetailsMap[userVoucher.voucherId] = it }
            }

            // Pair each user voucher with its voucher details
            userVouchers.mapNotNull { userVoucher ->
                val voucher = voucherDetailsMap[userVoucher.voucherId] ?: return@mapNotNull null
                Pair(userVoucher, voucher)
            }
        } catch (e: Exception) {
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
                isUsed = false
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
                        "isUsed" to true,
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