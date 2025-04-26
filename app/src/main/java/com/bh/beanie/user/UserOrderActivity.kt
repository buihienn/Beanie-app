package com.bh.beanie.user

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.databinding.ActivityUserOrderBinding
import com.bh.beanie.model.Branch
import com.bh.beanie.model.Category
import com.bh.beanie.model.Product
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.repository.CategoryRepository
import com.bh.beanie.repository.FavoriteRepository
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.user.adapter.ProductAdapter
import com.bh.beanie.user.fragment.ConfirmOrderFragment
import com.bh.beanie.user.fragment.SelectAddressFragment
import com.bh.beanie.user.fragment.SelectBranchFragment
//import com.bh.beanie.user.fragment.OrderConfirmationBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class UserOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOrderBinding
    private lateinit var productRepository: ProductRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var branchRepository: BranchRepository

    private var currentBranchId = ""
    private val branchList = mutableListOf<Branch>()

    // Các danh sách sản phẩm theo danh mục
    private val favProducts = mutableListOf<Product>()
    private val bestSellerProducts = mutableListOf<Product>()
    private val categoryProducts = mutableMapOf<String, MutableList<Product>>()
    private val categoryAdapters = mutableMapOf<String, ProductAdapter>()

    // Giữ danh sách categories
    private val categories = mutableListOf<Category>()

    private var orderMode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.order)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPrefs = this.getSharedPreferences("OrderMode", MODE_PRIVATE)
        orderMode = sharedPrefs.getString("order_mode", "") ?: ""

        if (orderMode == "take_away") {
            binding.orderMode.text = "Take away"
            loadSelectedBranch()
        } else {
            binding.orderMode.text = "Delivery"
            loadSelectedAddress()
        }

        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        currentBranchId = sharedPreferences.getString("selected_branch_id", "") ?: ""

        initializeRepositories()

        setupBranchSelector()

        loadCartCount()

        binding.cartBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val orderRepository = OrderRepository(FirebaseFirestore.getInstance(), this@UserOrderActivity)
                    val cartItems = orderRepository.getCartItems()

                    if (cartItems.isEmpty()) {
                        Toast.makeText(this@UserOrderActivity, "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi đặt hàng.", Toast.LENGTH_SHORT).show()
                    } else {
                        showConfirmOrder()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@UserOrderActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Categories"

        // Thiết lập RecyclerViews cố định
        setupFixedRecyclerViews()

        // Tải categories và sản phẩm từ Firebase
        loadCategories()
    }

    private fun showConfirmOrder() {
        val confirmOrderFragment = ConfirmOrderFragment.newInstance()
        confirmOrderFragment.show(supportFragmentManager, ConfirmOrderFragment.TAG)
    }

    // Lắng nghe thay đổi
    override fun onResume() {
        super.onResume()
        if (orderMode == "take_away") {
            loadSelectedBranch()
        } else {
            loadSelectedAddress()
        }
    }

    private fun initializeRepositories() {
        val firestore = FirebaseFirestore.getInstance()
        productRepository = ProductRepository(firestore)
        favoriteRepository = FavoriteRepository(firestore)
        categoryRepository = CategoryRepository(firestore)
        branchRepository = BranchRepository(firestore)
    }

    private fun setupBranchSelector() {
        // Thiết lập sự kiện click cho bộ chọn chi nhánh
        binding.branchSelectorLayout.setOnClickListener {
            if (branchList.isNotEmpty()) {
                showBranchSelectionDialog()
            } else {
                Toast.makeText(this, "Đang tải danh sách chi nhánh...", Toast.LENGTH_SHORT).show()
            }
        }

        // Tải danh sách chi nhánh từ repository
        loadBranches()
    }

    private fun loadBranches() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val branches = branchRepository.fetchActiveBranches()
                branchList.clear()
                branchList.addAll(branches)

                // Nếu chưa chọn chi nhánh, chọn chi nhánh đầu tiên
                if (currentBranchId.isEmpty() && branches.isNotEmpty()) {
                    currentBranchId = branches[0].id
                }

                // Cập nhật text hiển thị chi nhánh
                updateBranchSelectorText()

                // Tải danh mục cho chi nhánh đã chọn
                loadCategories()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Lỗi khi tải branch: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSelectedAddress() {
        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        val addressDisplay = sharedPreferences.getString("selected_address_display", "")

        // Cập nhật TextView hiển thị địa chỉ
        binding.addressText.text = addressDisplay ?: "Chọn địa chỉ giao hàng"

        // Thiết lập sự kiện khi click vào địa chỉ để chọn địa chỉ khác
        binding.addressText.setOnClickListener {
            showSelectAddressFragment()
        }

        binding.addressDropdownIcon.setOnClickListener {
            showSelectAddressFragment()
        }
    }

    private fun showSelectAddressFragment() {
        val fragment = SelectAddressFragment.newInstance()
        fragment.setAddressSelectedListener { address ->
            // Cập nhật địa chỉ hiển thị khi có địa chỉ mới được chọn
            binding.addressText.text = address.addressDetail
        }
        fragment.show(supportFragmentManager, "SelectAddressFragment")
    }

    private fun loadSelectedBranch() {
        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        val branchName = sharedPreferences.getString("selected_branch_name", "")
        val branchLocation = sharedPreferences.getString("selected_branch_location", "")

        // Cập nhật TextView hiển thị chi nhánh ở dưới
        val displayText = if (branchName?.isNotEmpty() == true) {
            "$branchName, $branchLocation"
        } else {
            "Chọn chi nhánh"
        }
        binding.addressText.text = displayText

        // Cập nhật TextView hiển thị chi nhánh ở toolbar
        binding.branchSelectorText.text = branchName ?: "Chọn chi nhánh"

        // Thiết lập sự kiện khi click vào để chọn chi nhánh khác
        binding.addressText.setOnClickListener {
            showSelectBranchFragment()
        }

        binding.addressDropdownIcon.setOnClickListener {
            showSelectBranchFragment()
        }
    }

    private fun showSelectBranchFragment() {
        val fragment = SelectBranchFragment.newInstance()
        fragment.setBranchSelectedListener { branch ->
            // Cập nhật thông tin chi nhánh trong cả toolbar và bottom bar
            currentBranchId = branch.id

            // Cập nhật text hiển thị ở cả hai vị trí
            binding.branchSelectorText.text = branch.name
            binding.addressText.text = "${branch.name}, ${branch.location}"

            // Tải lại dữ liệu theo chi nhánh mới được chọn
            reloadDataForSelectedBranch()
        }
        fragment.show(supportFragmentManager, "SelectBranchFragment")
    }

    private fun showBranchSelectionDialog() {
        // Thay thế bằng showSelectBranchFragment để đồng nhất UI và logic
        showSelectBranchFragment()
    }

    private fun updateBranchSelectorText() {
        val sharedPreferences = getSharedPreferences("BeaniePref", MODE_PRIVATE)
        val branchName = sharedPreferences.getString("selected_branch_name", "")
        binding.branchSelectorText.text = branchName ?: "Chọn chi nhánh"
    }

    private fun setupFixedRecyclerViews() {
        // Thiết lập adapter và RecyclerView cho Your fav
        val favAdapter = ProductAdapter(
            this,
            favProducts,
            currentBranchId,
            "favorites"  // Dùng category đặc biệt cho yêu thích
        )
        favAdapter.setOnCartUpdateListener(object : ProductAdapter.OnCartUpdateListener {
            override fun onCartCountUpdated(count: Int) {
                updateCartCount(count)
            }
        })
        binding.yourFavRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = favAdapter
        }

        // Thiết lập adapter và RecyclerView cho Best seller
        val bestSellerAdapter = ProductAdapter(
            this,
            bestSellerProducts,
            currentBranchId,
            "bestSellers"  // Dùng category đặc biệt cho best seller
        )
        bestSellerAdapter.setOnCartUpdateListener(object : ProductAdapter.OnCartUpdateListener {
            override fun onCartCountUpdated(count: Int) {
                updateCartCount(count)
            }
        })
        binding.bestSellerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserOrderActivity, 2)
            adapter = bestSellerAdapter
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

            // Tạo adapter với branchId và categoryId
            val adapter = ProductAdapter(
                this,
                productList,
                currentBranchId,
                category.id
            )
            adapter.setOnCartUpdateListener(object : ProductAdapter.OnCartUpdateListener {
                override fun onCartCountUpdated(count: Int) {
                    updateCartCount(count)
                }
            })
            categoryAdapters[category.id] = adapter

            // Inflate layout cho category
            val categoryView = layoutInflater.inflate(
                R.layout.category_section_layout,
                binding.categoriesContainer,
                false
            )

            // Thiết lập title và RecyclerView
            val titleTextView = categoryView.findViewById<TextView>(
                R.id.categoryTitleTextView
            )
            titleTextView.text = category.name

            val recyclerView = categoryView.findViewById<RecyclerView>(
                R.id.categoryRecyclerView
            )
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = adapter

            // Thêm vào container
            binding.categoriesContainer.addView(categoryView)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadDataForSelectedBranch() {
        // Hiển thị loading
        binding.progressBar.visibility = View.VISIBLE

        // Xóa dữ liệu hiện tại
        bestSellerProducts.clear()
        favProducts.clear()
        categories.clear()
        categoryProducts.clear()
        binding.categoriesContainer.removeAllViews()

        // Cập nhật adapter với branchId mới
        setupFixedRecyclerViews()

        // Tải lại danh mục và sản phẩm
        loadCategories()
    }

    private fun loadProductData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Sử dụng chi nhánh đã chọn
        val branchId = currentBranchId

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
                Toast.makeText(baseContext, "Lỗi khi tải dữ liệu: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadCategories() {
        // Cập nhật để sử dụng currentBranchId thay vì giá trị cứng
        if (currentBranchId.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Xóa dữ liệu hiện tại
        bestSellerProducts.clear()
        favProducts.clear()
        categories.clear()
        categoryProducts.clear()
        binding.categoriesContainer.removeAllViews()

        // Cập nhật adapters
        binding.bestSellerRecyclerView.adapter?.notifyDataSetChanged()
        binding.yourFavRecyclerView.adapter?.notifyDataSetChanged()

        lifecycleScope.launch {
            try {
                // Sử dụng ID chi nhánh đã chọn
                val fetchedCategories = categoryRepository.fetchCategories(currentBranchId)
                categories.clear()
                categories.addAll(fetchedCategories)

                // Thiết lập RecyclerViews cho từng category
                setupCategoryRecyclerViews()

                // Tải dữ liệu sản phẩm cho chi nhánh đã chọn
                loadProductData()

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Lỗi khi tải categories: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    // Cập nhật hiển thị số lượng sản phẩm trong giỏ hàng
    fun updateCartCount(count: Int) {
        if (count > 0) {
            binding.cartCountText.visibility = View.VISIBLE
            binding.cartCountText.text = count.toString()
        } else {
            binding.cartCountText.visibility = View.GONE
        }
    }

    // Tải số lượng sản phẩm trong giỏ hàng khi mở màn hình
    private fun loadCartCount() {
        val orderRepository = OrderRepository(FirebaseFirestore.getInstance(), this)
        lifecycleScope.launch {
            try {
                val cartItems = orderRepository.getCartItems()
                val totalCount = cartItems.sumOf { it.quantity }
                updateCartCount(totalCount)
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
            }
        }
    }
}