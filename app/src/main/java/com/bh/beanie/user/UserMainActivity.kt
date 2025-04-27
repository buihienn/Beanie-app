package com.bh.beanie.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.customer.LoginActivity
import com.bh.beanie.databinding.ActivityUserMainBinding
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.user.fragment.HomeFragment
import com.bh.beanie.user.fragment.OrderDetailFragment
import com.bh.beanie.user.fragment.OrderFragment
import com.bh.beanie.user.fragment.VoucherFragment
import com.bh.beanie.user.fragment.OtherFragment
import com.bh.beanie.user.fragment.RewardFragment
import com.bh.beanie.utils.OrderStatusManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class UserMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserMainBinding
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

        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (userId.isEmpty()) {
            // No user ID - go back to login
            Toast.makeText(this, "User authentication error", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }

        binding.orderStatusLayout.visibility = View.GONE
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

    override fun onStart() {
        super.onStart()
        OrderStatusManager.startListening { order ->
            if (order != null) {
                binding.orderStatusLayout.visibility = View.VISIBLE

                // Cập nhật status text dựa trên trạng thái đơn hàng
                binding.orderStatusTextView.text = when (order.status) {
                    "WAITING ACCEPT" -> "Your order is waiting acceptance"
                    "READY FOR PICKUP" -> "Your order is ready for pickup"
                    "PENDING" -> "Your order is pending"
                    "DELIVERING" -> "Your order is being delivered"
                    else -> "Your order is processing..."
                }

                // Cập nhật địa chỉ dựa trên loại đơn hàng
                if (order.type == "DELIVERY") {
                    binding.orderAddressTextView.text = order.deliveryAddress
                } else {
                    // Lấy thông tin chi nhánh
                    lifecycleScope.launch {
                        try {
                            val branchRepository = BranchRepository(FirebaseFirestore.getInstance())
                            val branch = branchRepository.fetchBranchById(order.branchId)
                            if (branch != null) {
                                binding.orderAddressTextView.text = branch.location
                            } else {
                                binding.orderAddressTextView.text = "At store"
                            }
                        } catch (e: Exception) {
                            Log.e("UserMainActivity", "Error loading branch: ${e.message}")
                            binding.orderAddressTextView.text = "At store"
                        }
                    }
                }
            } else {
                binding.orderStatusLayout.visibility = View.GONE
            }
        }

        binding.orderStatusLayout.setOnClickListener {
            OrderStatusManager.currentOrder?.let { order ->
                val orderDetailFragment = OrderDetailFragment.newInstance(order.id)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, orderDetailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        OrderStatusManager.stopListening()
    }
}