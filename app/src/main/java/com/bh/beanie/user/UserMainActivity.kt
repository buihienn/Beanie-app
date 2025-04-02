package com.bh.beanie.user

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.R
import com.bh.beanie.user.fragment.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserMainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ánh xạ bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            // Chọn fragment tương ứng với item được nhấn
            val selectedFragment = when (item.itemId) {
                R.id.navigation_home -> HomeFragment.newInstance("hihi", "haha")
                R.id.navigation_order -> OrderFragment.newInstance("hihi", "haha")
                R.id.navigation_reward -> RewardFragment.newInstance("hihi", "haha")
                R.id.navigation_voucher -> VoucherFragment.newInstance("hihi", "haha")
                R.id.navigation_other -> OtherFragment.newInstance("hihi", "haha")
                // Thêm các fragment khác
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
}