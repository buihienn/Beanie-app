package com.bh.beanie.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    // Order mdoe
    private var orderMode = ""

    // Thêm các biến để theo dõi pagination
    private var lastVisibleCategory: DocumentSnapshot? = null
    private var isCategoryLoading = false
    private var isLastCategoryPage = false

    // Map lưu trữ lastVisibleDocument cho mỗi category
    private val lastVisibleProducts = mutableMapOf<String, DocumentSnapshot?>()
    private val isProductLoading = mutableMapOf<String, Boolean>()
    private val isLastProductPage = mutableMapOf<String, Boolean>()

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

        setupPagination()

        // Tải categories đầu tiên
        loadInitialCategories()
    }

    private fun setupPagination() {
        // Thiết lập scroll listener cho nested scrollview để detect khi scroll xuống cuối
        binding.mainScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val view = v as NestedScrollView
            val diffY = scrollY - oldScrollY

            if (diffY > 0) {
                // Scrolling down
                val child = view.getChildAt(0)
                if (child != null) {
                    if ((view.height + scrollY) >= child.measuredHeight - 200) {
                        // Đã scroll gần cuối, tải thêm categories
//                        if (!isCategoryLoading && !isLastCategoryPage) {
                            loadMoreCategories()
//                        }
                    }
                }
            }
        }
    }

    private fun findCategoryViewById(categoryId: String): View? {
        for (i in 0 until binding.categoriesContainer.childCount) {
            val categoryView = binding.categoriesContainer.getChildAt(i)
            val titleTextView = categoryView.findViewById<TextView>(R.id.categoryTitleTextView)
            val recyclerView = categoryView.findViewById<RecyclerView>(R.id.categoryRecyclerView)
            val adapter = recyclerView.adapter
            if (adapter is ProductAdapter) {
                val adapterCategoryId = categoryAdapters.entries.find { it.value == adapter }?.key
                if (adapterCategoryId == categoryId) {
                    return categoryView
                }
            }
        }
        return null
    }

    private fun loadInitialCategories() {
        if (currentBranchId.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        isCategoryLoading = true

        lifecycleScope.launch {
            try {
                val (fetchedCategories, lastDoc) = categoryRepository.fetchCategoriesPaginated(currentBranchId)

                categories.clear()
                categories.addAll(fetchedCategories)
                lastVisibleCategory = lastDoc
                isLastCategoryPage = fetchedCategories.isEmpty()

                // Thiết lập RecyclerViews cho từng category
                setupCategoryRecyclerViews()

                // Tải product cho mỗi category
                for (category in fetchedCategories) {
                    loadInitialProductsForCategory(category.id)
                }

                // Tải dữ liệu cố định (favorites, bestsellers)
                loadProductData()

            } catch (e: Exception) {
                Toast.makeText(baseContext, "Lỗi khi tải categories: ${e.message}",
                    Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isCategoryLoading = false
            }
        }
    }

    private fun loadMoreCategories() {
        if (isCategoryLoading || isLastCategoryPage) return

        binding.loadMoreCategoriesProgress.visibility = View.VISIBLE
        isCategoryLoading = true

        lifecycleScope.launch {
            try {
                val (fetchedCategories, lastDoc) = categoryRepository.fetchCategoriesPaginated(
                    currentBranchId,
                    lastVisibleCategory
                )

                if (fetchedCategories.isEmpty()) {
                    isLastCategoryPage = true
                } else {
                    // Lưu vị trí cuối cùng để tải tiếp
                    lastVisibleCategory = lastDoc

                    // Thêm vào danh sách categories
                    categories.addAll(fetchedCategories)

                    // Tạo và thêm views cho categories mới
                    addNewCategoryViews(fetchedCategories)

                    // Tải products cho các categories mới
                    for (category in fetchedCategories) {
                        loadInitialProductsForCategory(category.id)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(baseContext, "Lỗi khi tải thêm categories: ${e.message}",
                    Toast.LENGTH_LONG).show()
            } finally {
                binding.loadMoreCategoriesProgress.visibility = View.GONE
                isCategoryLoading = false
            }
        }
    }

    private fun addNewCategoryViews(newCategories: List<Category>) {
        for (category in newCategories) {
            // Tạo danh sách sản phẩm cho category này
            val productList = mutableListOf<Product>()
            categoryProducts[category.id] = productList

            // Tạo adapter
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
            val titleTextView = categoryView.findViewById<TextView>(R.id.categoryTitleTextView)
            titleTextView.text = category.name

            val recyclerView = categoryView.findViewById<RecyclerView>(R.id.categoryRecyclerView)
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = adapter

            // Thiết lập listener cuộn ngang cho RecyclerView
            setupProductPagination(category.id, recyclerView)

            // Thêm vào container
            binding.categoriesContainer.addView(categoryView)
        }
    }

    private fun setupProductPagination(categoryId: String, recyclerView: RecyclerView) {
        // Khởi tạo trạng thái cho category này
        isProductLoading[categoryId] = false
        isLastProductPage[categoryId] = false

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (dx > 0) { // Scroll to right
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                        && firstVisibleItemPosition >= 0
                        && !isProductLoading.getOrDefault(categoryId, false)
                        && !isLastProductPage.getOrDefault(categoryId, false)) {

                        loadMoreProductsForCategory(categoryId)
                    }
                }
            }
        })
    }

    private fun loadInitialProductsForCategory(categoryId: String) {
        isProductLoading[categoryId] = true

        lifecycleScope.launch {
            try {
                val (products, lastDoc) = productRepository.fetchProductsPaginated(
                    currentBranchId,
                    categoryId
                )

                lastVisibleProducts[categoryId] = lastDoc
                isLastProductPage[categoryId] = products.isEmpty()

                categoryProducts[categoryId]?.clear()
                categoryProducts[categoryId]?.addAll(products)
                categoryAdapters[categoryId]?.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e("UserOrderActivity", "Error loading products for category $categoryId: ${e.message}")
            } finally {
                isProductLoading[categoryId] = false
            }
        }
    }

    private fun loadMoreProductsForCategory(categoryId: String) {
        val isLoading = isProductLoading.getOrDefault(categoryId, false)
        val isLastPage = isLastProductPage.getOrDefault(categoryId, false)

        if (isLoading || isLastPage) return

        isProductLoading[categoryId] = true

        // Tìm loading container của category hiện tại
        val categoryView = findCategoryViewById(categoryId)
        val loadingContainer = categoryView?.findViewById<View>(R.id.productLoadingContainer)
        loadingContainer?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val lastVisible = lastVisibleProducts[categoryId]
                val (products, lastDoc) = productRepository.fetchProductsPaginated(
                    currentBranchId,
                    categoryId,
                    lastVisible
                )

                if (products.isEmpty()) {
                    isLastProductPage[categoryId] = true
                } else {
                    lastVisibleProducts[categoryId] = lastDoc

                    // Lấy vị trí bắt đầu để thêm
                    val startPosition = categoryProducts[categoryId]?.size ?: 0

                    // Thêm sản phẩm mới vào danh sách
                    categoryProducts[categoryId]?.addAll(products)

                    // Thông báo adapter về các mục đã thêm mới
                    categoryAdapters[categoryId]?.notifyItemRangeInserted(startPosition, products.size)
                }

            } catch (e: Exception) {
                Log.e("UserOrderActivity", "Error loading more products for category $categoryId: ${e.message}")
            } finally {
                isProductLoading[categoryId] = false
                loadingContainer?.visibility = View.GONE
            }
        }
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
        if (currentBranchId.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset trạng thái pagination
        lastVisibleCategory = null
        isLastCategoryPage = false
        lastVisibleProducts.clear()
        isProductLoading.clear()
        isLastProductPage.clear()

        // Xóa dữ liệu hiện tại
        bestSellerProducts.clear()
        favProducts.clear()
        categories.clear()
        categoryProducts.clear()
        binding.categoriesContainer.removeAllViews()

        // Cập nhật adapters
        binding.bestSellerRecyclerView.adapter?.notifyDataSetChanged()
        binding.yourFavRecyclerView.adapter?.notifyDataSetChanged()

        // Gọi đến loadInitialCategories để tải dữ liệu theo trang
        loadInitialCategories()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Cập nhật intent mới cho Activity

        val confirmOrderFragment = supportFragmentManager.findFragmentByTag(ConfirmOrderFragment.TAG) as? ConfirmOrderFragment
        confirmOrderFragment?.handlePayPalResult(intent)
    }

}