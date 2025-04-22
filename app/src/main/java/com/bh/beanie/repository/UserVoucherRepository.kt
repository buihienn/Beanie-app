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
                // .whereEqualTo("isUsed", false)
                .get()
                .await()

            Log.d("UserVoucherRepository", "Query returned ${userVouchersSnapshot.size()} documents")

            // Log all raw document data
            userVouchersSnapshot.documents.forEachIndexed { index, doc ->
                Log.d("UserVoucherRepository", "Raw document $index (ID: ${doc.id}): ${doc.data}")
            }

            val userVouchers = userVouchersSnapshot.documents.mapNotNull {
                try {
                    val userVoucher = it.toObject(UserVoucher::class.java)
                    Log.d("UserVoucherRepository", "Mapped document ${it.id} to UserVoucher: $userVoucher")
                    userVoucher
                } catch (e: Exception) {
                    Log.e("UserVoucherRepository", "Error mapping document ${it.id}: ${e.message}")
                    null
                }
            }

            Log.d("UserVoucherRepository", "Successfully mapped ${userVouchers.size} user vouchers")

            if (userVouchers.isEmpty()) {
                Log.d("UserVoucherRepository", "No user vouchers found for user $currentUserId")
                return emptyList()
            }

            // Get full voucher details for each user voucher
            val voucherDetailsMap = mutableMapOf<String, Voucher>()

            for (userVoucher in userVouchers) {
                if (voucherDetailsMap.containsKey(userVoucher.voucherId)) {
                    Log.d("UserVoucherRepository", "Using cached voucher details for voucherId: ${userVoucher.voucherId}")
                    continue
                }

                Log.d("UserVoucherRepository", "Fetching voucher details for voucherId: ${userVoucher.voucherId}")
                val voucherSnapshot = vouchersCollection.document(userVoucher.voucherId).get().await()

                if (!voucherSnapshot.exists()) {
                    Log.w("UserVoucherRepository", "Voucher document does not exist for ID: ${userVoucher.voucherId}")
                    continue
                }

                Log.d("UserVoucherRepository", "Raw voucher data: ${voucherSnapshot.data}")

                val voucher = voucherSnapshot.toObject(Voucher::class.java)
                if (voucher != null) {
                    Log.d("UserVoucherRepository", "Successfully mapped voucher: $voucher")
                    voucherDetailsMap[userVoucher.voucherId] = voucher
                } else {
                    Log.w("UserVoucherRepository", "Failed to map voucher data for ID: ${userVoucher.voucherId}")
                }
            }

            Log.d("UserVoucherRepository", "Retrieved ${voucherDetailsMap.size} voucher details")

            // Pair each user voucher with its voucher details
            val result = userVouchers.mapNotNull { userVoucher ->
                val voucher = voucherDetailsMap[userVoucher.voucherId]
                if (voucher == null) {
                    Log.w("UserVoucherRepository", "No voucher found for userVoucher with voucherId: ${userVoucher.voucherId}")
                    null
                } else {
                    Log.d("UserVoucherRepository", "Pairing userVoucher ${userVoucher.id} with voucher ${voucher.id}")
                    Pair(userVoucher, voucher)
                }
            }

            Log.d("UserVoucherRepository", "Final result: ${result.size} paired vouchers")
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