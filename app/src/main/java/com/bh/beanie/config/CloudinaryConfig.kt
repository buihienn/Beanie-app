package com.bh.beanie.config

import android.content.Context
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.bh.beanie.BuildConfig

object CloudinaryConfig {

    private var cloudinary: Cloudinary? = null

    fun initialize(context: Context) {
        if (cloudinary == null) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(context.applicationContext, config)
            cloudinary = MediaManager.get().cloudinary
        }
    }

    fun getCloudinaryInstance(): Cloudinary {
        return cloudinary ?: throw IllegalStateException("CloudinaryConfig is not initialized. Call initialize(context) first.")
    }
}
