package com.bh.beanie.repository

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.util.*

class CloudinaryRepository {

    fun uploadImage(
        filePath: String,
        folderName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uploadId = UUID.randomUUID().toString()

        MediaManager.get().upload(filePath)
            .unsigned("beanie-image") // Replace with your unsigned preset
            .option("folder", folderName) // Specify the folder name
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Optional: Track upload progress
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    if (imageUrl != null) {
                        onSuccess(imageUrl)
                    } else {
                        onFailure(Exception("Failed to retrieve image URL"))
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload error: $requestId, ${error.description}")
                    onFailure(Exception(error.description))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload rescheduled: $requestId, ${error.description}")
                }
            })
            .dispatch()
    }
}