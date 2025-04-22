package com.bh.beanie.repository

import android.util.Log
import com.bh.beanie.model.Branch
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BranchRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // Fetch tất cả chi nhánh
    suspend fun fetchBranches(): List<Branch> {
        return try {
            val snapshot = db.collection("branches").get().await()

            snapshot.documents.map { doc ->
                Branch(
                    phone = doc.getString("phone") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    location = doc.getString("location") ?: "",
                    adminId = doc.getString("adminId") ?: "",
                    name = doc.getString("name") ?: ""
                ).apply {
                    // Lưu ID document
                    this.id = doc.id
                }
            }
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi fetch branches: ${e.message}")
            emptyList()
        }
    }

    // Fetch các chi nhánh đang hoạt động
    suspend fun fetchActiveBranches(): List<Branch> {
        return try {
            val snapshot = db.collection("branches")
                .whereEqualTo("isActivited", true)
                .get()
                .await()

            snapshot.documents.map { doc ->
                Branch(
                    phone = doc.getString("phone") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isActive = true,
                    location = doc.getString("location") ?: "",
                    adminId = doc.getString("adminId") ?: "",
                    name = doc.getString("name") ?: ""
                ).apply {
                    this.id = doc.id
                }
            }
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi fetch active branches: ${e.message}")
            emptyList()
        }
    }

    // Fetch chi nhánh theo ID
    suspend fun fetchBranchById(branchId: String): Branch? {
        return try {
            val doc = db.collection("branches").document(branchId).get().await()
            if (doc.exists()) {
                Branch(
                    phone = doc.getString("phone") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    location = doc.getString("location") ?: "",
                    adminId = doc.getString("adminId") ?: "",
                    name = doc.getString("name") ?: ""
                ).apply {
                    this.id = doc.id
                }
            } else null
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi fetch branch $branchId: ${e.message}")
            null
        }
    }

    // Fetch branches với phân trang
    suspend fun fetchBranchesPaginated(lastVisibleDocument: DocumentSnapshot? = null): Pair<List<Branch>, DocumentSnapshot?> {
        return try {
            var query = db.collection("branches")
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(10)

            lastVisibleDocument?.let {
                query = query.startAfter(it)
            }

            val snapshot = query.get().await()

            val branches = snapshot.documents.map { doc ->
                Branch(
                    phone = doc.getString("phone") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    location = doc.getString("location") ?: "",
                    adminId = doc.getString("adminId") ?: "",
                    name = doc.getString("name") ?: ""
                ).apply {
                    this.id = doc.id
                }
            }

            val lastDoc = snapshot.documents.lastOrNull()
            Pair(branches, lastDoc)
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi fetch paginated branches: ${e.message}")
            Pair(emptyList(), null)
        }
    }

    // Thêm chi nhánh mới
    suspend fun addBranch(branch: Branch): String {
        return try {
            val branchRef = if (branch.id.isNotEmpty()) {
                db.collection("branches").document(branch.id)
            } else {
                db.collection("branches").document()
            }

            val branchData = mapOf(
                "phone" to branch.phone,
                "imageUrl" to branch.imageUrl,
                "isActive" to branch.isActive,
                "location" to branch.location,
                "adminId" to branch.adminId,
                "name" to branch.name
            )

            branchRef.set(branchData).await()
            branchRef.id
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi thêm branch: ${e.message}")
            throw e
        }
    }

    // Cập nhật chi nhánh
    suspend fun updateBranch(branch: Branch) {
        try {
            val branchRef = db.collection("branches").document(branch.id)

            val updatedData = mapOf(
                "phone" to branch.phone,
                "imageUrl" to branch.imageUrl,
                "isActive" to branch.isActive,
                "location" to branch.location,
                "adminId" to branch.adminId,
                "name" to branch.name
            )

            branchRef.update(updatedData).await()
            Log.d("BranchRepository", "Branch cập nhật thành công: ${branch.id}")
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi cập nhật branch: ${e.message}")
            throw e
        }
    }

    // Xóa chi nhánh
    suspend fun deleteBranch(branchId: String) {
        try {
            db.collection("branches").document(branchId).delete().await()
            Log.d("BranchRepository", "Branch xóa thành công: $branchId")
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi xóa branch: ${e.message}")
            throw e
        }
    }

    // Chuyển trạng thái hoạt động của chi nhánh
    suspend fun toggleBranchActive(branchId: String, isActive: Boolean) {
        try {
            val branchRef = db.collection("branches").document(branchId)
            branchRef.update("isActive", isActive).await()
            Log.d("BranchRepository", "Trạng thái branch cập nhật: $branchId, isActive: $isActive")
        } catch (e: Exception) {
            Log.e("BranchRepository", "Lỗi khi thay đổi trạng thái branch: ${e.message}")
            throw e
        }
    }
}