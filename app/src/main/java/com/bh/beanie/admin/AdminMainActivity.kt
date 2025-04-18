package com.bh.beanie.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.R
import com.bh.beanie.admin.fragments.AdminDashBoardFragment
import com.bh.beanie.admin.fragments.AdminInventoryFragment
import com.bh.beanie.admin.fragments.AdminOrderFragment
import com.bh.beanie.admin.fragments.AdminVoucherFragment
import com.bh.beanie.config.CloudinaryConfig
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class AdminMainActivity : AppCompatActivity() {


    private lateinit var bottomNav: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CloudinaryConfig.initialize(this)
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
                R.id.orderAdmin -> AdminOrderFragment()
                R.id.inventoryAdmin -> AdminInventoryFragment()

                // Thêm Fragment khác nếu cần
                else -> null
            }

            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .addToBackStack(null) // Thêm fragment vào back stack
                    .commit()
                true
            } ?: false
        }
    }


}