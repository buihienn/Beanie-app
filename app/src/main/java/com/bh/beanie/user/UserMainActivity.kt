package com.bh.beanie.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.R
import com.bh.beanie.customer.LoginActivity
import com.bh.beanie.user.fragment.HomeFragment
import com.bh.beanie.user.fragment.OrderFragment
import com.bh.beanie.user.fragment.VoucherFragment
import com.bh.beanie.user.fragment.OtherFragment
import com.bh.beanie.user.fragment.RewardFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserMainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    //User information
    private lateinit var userId: String
    private var userEmail: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = intent.getStringExtra("USER_ID") ?: ""
        userEmail = intent.getStringExtra("USER_EMAIL")
        userName = intent.getStringExtra("USER_NAME")

        Log.d("UserMainActivity", "Received user data: ID=$userId, Email=$userEmail, Name=$userName")

        if (userId.isEmpty()) {
            // No user ID - go back to login
            Toast.makeText(this, "User authentication error", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Ánh xạ bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            // Chọn fragment tương ứng với item được nhấn
            val selectedFragment = when (item.itemId) {
                R.id.navigation_home -> HomeFragment.newInstance()
                R.id.navigation_order -> OrderFragment.newInstance()
                R.id.navigation_reward -> RewardFragment.newInstance("param1", "param2")
                R.id.navigation_voucher -> VoucherFragment.newInstance(userId)
                R.id.navigation_other -> OtherFragment.newInstance("param1", "param2")
                else -> null
            }

            // Nếu có fragment được chọn, thay thế fragment hiện tại
            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
            }

            // Trả về true nếu item được chọn
            selectedFragment != null
        }

        // Mặc định hiển thị HomeFragment khi khởi động ứng dụng
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }

    fun getUserId(): String {
        return userId
    }

    // Method to get user information (can be expanded)
    fun getUserInfo(): Map<String, String?> {
        return mapOf(
            "id" to userId,
            "email" to userEmail,
            "name" to userName
        )
    }
}