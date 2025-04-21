package com.bh.beanie

import com.bh.beanie.utils.UserPreferences
import android.app.Application

class BeanieApplication : Application() {
    companion object {
        lateinit var instance: BeanieApplication
            private set
    }

    private var userId: String = ""

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Khôi phục userId từ SharedPreferences
        userId = UserPreferences.getUserId(this)
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
}