package com.bh.beanie

import com.bh.beanie.utils.UserPreferences
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class BeanieApplication : Application() {
    companion object {
        lateinit var instance: BeanieApplication
            private set
    }

    private var userId: String = ""

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Set persistence correctly - this MUST happen before any Firebase Auth initialization
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            // Note: For FirebaseAuth, persistence is enabled by default in Android
        } catch (e: Exception) {
            Log.e("BeanieApplication", "Error setting Firebase persistence", e)
        }

        // Restore userId from SharedPreferences
        userId = UserPreferences.getUserId(this)

        // Apply saved language setting
        val prefs = getSharedPreferences("LanguageSettings", MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        updateLanguage(lang)
    }

    fun setUserId(id: String) {
        userId = id
        // Lưu vào SharedPreferences để duy trì giữa các lần khởi động
        UserPreferences.saveUserId(this, id)
    }

    fun getUserId(): String {
        return userId
    }

    fun clearUserId() {
        userId = ""
        UserPreferences.clearUserId(this)
    }

    private fun updateLanguage(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}