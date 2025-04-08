package com.bh.beanie.user

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.GridLayoutManager
import com.bh.beanie.R
import com.bh.beanie.databinding.ActivityUserOrderBinding
import com.bh.beanie.user.adapter.ProductAdapter
import com.bh.beanie.user.fragment.OrderConfirmationBottomSheet
import com.bh.beanie.user.fragment.SelectAddressFragment
import com.bh.beanie.user.model.Product

class UserOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOrderBinding

    // Các danh sách sản phẩm theo danh mục
    private val favProducts = mutableListOf<Product>()
    private val bestSellerProducts = mutableListOf<Product>()
    private val coffeeProducts = mutableListOf<Product>()

    // Các adapter cho danh sách sản phẩm
    private lateinit var favAdapter: ProductAdapter
    private lateinit var bestSellerAdapter: ProductAdapter
    private lateinit var coffeeAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cartBtn.setOnClickListener {
            try {
                val orderConfirmationSheet = OrderConfirmationBottomSheet.newInstance()
                orderConfirmationSheet.show(supportFragmentManager, OrderConfirmationBottomSheet.TAG)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Categories"

        // Lấy địa chỉ đã chọn từ SharedPreferences
        loadSelectedAddress()

        // Tạo dữ liệu mẫu
        createSampleData()

        // Thiết lập RecyclerViews
        setupRecyclerViews()
    }

    private fun loadSelectedAddress() {
        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        val addressDisplay = sharedPreferences.getString("selected_address_display", "")

        // Hiển thị địa chỉ đã chọn nếu có TextView hiển thị địa chỉ trong layout
        // binding.addressText.text = addressDisplay
    }

    private fun createSampleData() {
        // Tạo sản phẩm yêu thích
        favProducts.apply {
            add(Product(1, "Matcha latte", 49000.0, R.drawable.matcha, true))
            add(Product(2, "Olong Blao Milktea", 35000.0, R.drawable.matcha, true))
        }

        // Tạo sản phẩm bán chạy
        bestSellerProducts.apply {
            add(Product(3, "Coffee extra milk", 35000.0, R.drawable.matcha, false))
            add(Product(4, "Olong Blao Milktea", 35000.0, R.drawable.matcha, false))
        }

        // Tạo sản phẩm cà phê
        coffeeProducts.apply {
            add(Product(5, "Coffee extra milk", 35000.0, R.drawable.matcha, false))
            add(Product(6, "Olong Blao Milktea", 35000.0, R.drawable.matcha, false))
        }
    }

    private fun setupRecyclerViews() {
        // Thiết lập adapter và RecyclerView cho Your fav
        favAdapter = ProductAdapter(this, favProducts)
        binding.yourFavRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = favAdapter
        }

        // Thiết lập adapter và RecyclerView cho Best seller
        bestSellerAdapter = ProductAdapter(this, bestSellerProducts)
        binding.bestSellerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = bestSellerAdapter
        }

        // Thiết lập adapter và RecyclerView cho Coffee
        coffeeAdapter = ProductAdapter(this, coffeeProducts)
        binding.coffeeRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = coffeeAdapter
        }
    }
}