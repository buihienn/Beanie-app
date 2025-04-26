package com.bh.beanie.admin


import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.R
import com.bh.beanie.admin.fragments.AdminDashBoardFragment
import com.bh.beanie.admin.fragments.AdminInventoryFragment
import com.bh.beanie.admin.fragments.AdminManageCustomer
import com.bh.beanie.admin.fragments.AdminOrderFragment
import com.bh.beanie.admin.fragments.AdminVoucherFragment
import com.bh.beanie.config.CloudinaryConfig
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminMainActivity : AppCompatActivity() {

    private lateinit var branchId: String
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var nameAdmin: String
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

        branchId = intent.getStringExtra("branchId") ?: "braches_q5"
        nameAdmin = intent.getStringExtra("nameAdmin") ?: "Admin"

        if (savedInstanceState == null) {
            val dashboardFragment = AdminDashBoardFragment()
            dashboardFragment.arguments = Bundle().apply {
                putString("branchId", branchId)
                putString("nameAdmin", nameAdmin)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.dashboardAdmin -> AdminDashBoardFragment()
                R.id.voucherAdmin -> AdminVoucherFragment()
                R.id.orderAdmin -> AdminOrderFragment()
                R.id.inventoryAdmin -> AdminInventoryFragment()
                R.id.otherAdmin -> AdminManageCustomer()


                // Thêm Fragment khác nếu cần
                else -> null
            }

            fragment?.arguments = Bundle().apply {
                putString("branchId", branchId)
                putString("nameAdmin", nameAdmin)
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