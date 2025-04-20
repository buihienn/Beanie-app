package com.bh.beanie.user

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bh.beanie.databinding.ActivityUserOrderBinding
import com.bh.beanie.model.Category
import com.bh.beanie.model.Product
import com.bh.beanie.repository.CategoryRepository
import com.bh.beanie.repository.FavoriteRepository
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.user.adapter.ProductAdapter
//import com.bh.beanie.user.fragment.OrderConfirmationBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class UserOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOrderBinding
    private lateinit var productRepository: ProductRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var categoryRepository: CategoryRepository

    // Các danh sách sản phẩm theo danh mục
    private val favProducts = mutableListOf<Product>()
    private val bestSellerProducts = mutableListOf<Product>()
    private val categoryProducts = mutableMapOf<String, MutableList<Product>>()
    private val categoryAdapters = mutableMapOf<String, ProductAdapter>()

    // Giữ danh sách categories
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeRepositories()

        binding.cartBtn.setOnClickListener {
            try {
//                val orderConfirmationSheet = OrderConfirmationBottomSheet.newInstance()
//                orderConfirmationSheet.show(supportFragmentManager, OrderConfirmationBottomSheet.TAG)
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

        // Thiết lập RecyclerViews cố định
        setupFixedRecyclerViews()

        // Hiển thị trạng thái loading
//        binding.progressBar.visibility = View.VISIBLE

        // Tải categories và sản phẩm từ Firebase
        loadCategories()
    }

    private fun initializeRepositories() {
        val firestore = FirebaseFirestore.getInstance()
        productRepository = ProductRepository(firestore)
        favoriteRepository = FavoriteRepository(firestore)
        categoryRepository = CategoryRepository(firestore)
    }

    private fun loadSelectedAddress() {
        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        val addressDisplay = sharedPreferences.getString("selected_address_display", "")
        // Hiển thị địa chỉ đã chọn nếu có TextView hiển thị địa chỉ trong layout
    }

    private fun setupFixedRecyclerViews() {
        // Thiết lập adapter và RecyclerView cho Your fav
        val favAdapter = ProductAdapter(this, favProducts)
        binding.yourFavRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = favAdapter
        }

        // Thiết lập adapter và RecyclerView cho Best seller
        val bestSellerAdapter = ProductAdapter(this, bestSellerProducts)
        binding.bestSellerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = bestSellerAdapter
        }
    }

    private fun loadCategories() {
        val branchId = "braches_q5" // Hoặc lấy từ settings/preferences

        lifecycleScope.launch {
            try {
                // Tải tất cả categories
                val fetchedCategories = categoryRepository.fetchCategories(branchId)
                categories.clear()
                categories.addAll(fetchedCategories)

                // Tạo các RecyclerView cho từng category
                setupCategoryRecyclerViews()

                // Tải dữ liệu sản phẩm cho từng category và các phần cố định
                loadProductData()

//                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
//                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@UserOrderActivity,
                    "Lỗi khi tải categories: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupCategoryRecyclerViews() {
        // Xóa các category layout đã tạo trước đó (nếu có)
        binding.categoriesContainer.removeAllViews()

        // Với mỗi category, tạo một layout Title + RecyclerView
        for (category in categories) {
            // Tạo danh sách sản phẩm cho category này
            val productList = mutableListOf<Product>()
            categoryProducts[category.id] = productList

            // Tạo adapter
            val adapter = ProductAdapter(this, productList)
            categoryAdapters[category.id] = adapter

            // Inflate layout cho category
            val categoryView = layoutInflater.inflate(
                com.bh.beanie.R.layout.category_section_layout,
                binding.categoriesContainer,
                false
            )

            // Thiết lập title và RecyclerView
            val titleTextView = categoryView.findViewById<android.widget.TextView>(
                com.bh.beanie.R.id.categoryTitleTextView
            )
            titleTextView.text = category.name

            val recyclerView = categoryView.findViewById<androidx.recyclerview.widget.RecyclerView>(
                com.bh.beanie.R.id.categoryRecyclerView
            )
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = adapter

            // Thêm vào container
            binding.categoriesContainer.addView(categoryView)
        }
    }

    private fun loadProductData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val branchId = "braches_q5" // Hoặc lấy từ settings/preferences

        lifecycleScope.launch {
            try {
                // Tải sản phẩm yêu thích
                if (userId != null) {
                    val favorites = favoriteRepository.getFavorites(userId)
                    favProducts.clear()
                    favProducts.addAll(favorites)
                    (binding.yourFavRecyclerView.adapter as ProductAdapter).notifyDataSetChanged()
                }

                // Tải sản phẩm bán chạy
                val bestSellers = productRepository.fetchBestSellersSuspend(branchId)
                bestSellerProducts.clear()
                bestSellerProducts.addAll(bestSellers)
                (binding.bestSellerRecyclerView.adapter as ProductAdapter).notifyDataSetChanged()

                // Tải sản phẩm cho từng category
                for (category in categories) {
                    val products = categoryRepository.fetchCategoryItems(branchId, category.id)
                    categoryProducts[category.id]?.clear()
                    categoryProducts[category.id]?.addAll(products)
                    categoryAdapters[category.id]?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@UserOrderActivity,
                    "Lỗi khi tải dữ liệu: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}