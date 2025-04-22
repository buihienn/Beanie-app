package com.bh.beanie.repository

import com.bh.beanie.model.Address
import com.google.firebase.firestore.FirebaseFirestore

class AddressRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getAddresses(userId: String, callback: (List<Address>) -> Unit) {
        db.collection("users").document(userId).collection("addresses")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val addresses = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Address::class.java)?.copy(id = doc.id)
                }
                callback(addresses)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun addAddress(userId: String, address: Address, callback: (Boolean, String?) -> Unit) {
        if (address.isDefault) {
            makeAddressDefault(userId, "", callback) { success ->
                if (success) {
                    addAddressToDb(userId, address, callback)
                } else {
                    callback(false, "Failed to update default status")
                }
            }
        } else {
            addAddressToDb(userId, address, callback)
        }
    }

    private fun addAddressToDb(userId: String, address: Address, callback: (Boolean, String?) -> Unit) {
        val addressWithoutId = hashMapOf(
            "nameAddress" to address.nameAddress,
            "name" to address.name,
            "phoneNumber" to address.phoneNumber,
            "addressDetail" to address.addressDetail,
            "isDefault" to address.isDefault
        )

        db.collection("users").document(userId).collection("addresses")
            .add(addressWithoutId)
            .addOnSuccessListener {
                callback(true, it.id)
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    fun updateAddress(userId: String, address: Address, callback: (Boolean, String?) -> Unit) {
        if (address.isDefault) {
            makeAddressDefault(userId, address.id, callback) { success ->
                if (success) {
                    updateAddressInDb(userId, address, callback)
                } else {
                    callback(false, "Failed to update default status")
                }
            }
        } else {
            updateAddressInDb(userId, address, callback)
        }
    }

    private fun updateAddressInDb(userId: String, address: Address, callback: (Boolean, String?) -> Unit) {
        val addressData = hashMapOf(
            "nameAddress" to address.nameAddress,
            "name" to address.name,
            "phoneNumber" to address.phoneNumber,
            "addressDetail" to address.addressDetail,
            "isDefault" to address.isDefault
        )

        db.collection("users").document(userId).collection("addresses").document(address.id)
            .update(addressData as Map<String, Any>)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    fun deleteAddress(userId: String, addressId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("users").document(userId).collection("addresses").document(addressId)
            .delete()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    fun makeAddressDefault(
        userId: String,
        exceptAddressId: String,
        mainCallback: (Boolean, String?) -> Unit,
        completion: (Boolean) -> Unit
    ) {
        db.collection("users").document(userId).collection("addresses")
            .whereEqualTo("isDefault", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                var hasUpdates = false

                for (doc in snapshot.documents) {
                    if (doc.id != exceptAddressId) {
                        batch.update(doc.reference, "isDefault", false)
                        hasUpdates = true
                    }
                }

                if (hasUpdates) {
                    batch.commit()
                        .addOnSuccessListener {
                            completion(true)
                        }
                        .addOnFailureListener {
                            mainCallback(false, it.message)
                            completion(false)
                        }
                } else {
                    completion(true)
                }
            }
            .addOnFailureListener {
                mainCallback(false, it.message)
                completion(false)
            }
    }
}