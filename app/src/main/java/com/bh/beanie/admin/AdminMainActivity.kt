package com.bh.beanie.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.R
import com.bh.beanie.admin.fragments.AdminDashBoardFragment
import com.bh.beanie.admin.fragments.AdminVoucherFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminMainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bottomNav = findViewById(R.id.bottomNavigationView)




        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminDashBoardFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.dashboardAdmin -> AdminDashBoardFragment()
                R.id.voucherAdmin -> AdminVoucherFragment()
                // Thêm Fragment khác nếu cần
                else -> null
            }

            fragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, it).commit()
                true
            } ?: false
        }
    }



}