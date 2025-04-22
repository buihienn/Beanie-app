package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.BeanieApplication
import com.bumptech.glide.Glide
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentProductDetailBinding
import com.bh.beanie.model.Product
import com.bh.beanie.model.ProductSize
import com.bh.beanie.model.ProductTopping
import com.bh.beanie.repository.FavoriteRepository
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.repository.ProductRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.compareTo

class ProductDetailFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: Product? = null
    private var quantity = 1
    private var selectedSize: ProductSize? = null
    private val selectedToppings = mutableListOf<ProductTopping>()
    private var toppingPrice = 0.0
    private var totalPrice = 0.0
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var productRepository: ProductRepository
    private var currentUserId: String? = null
    private var isFavorite = false

    private var branchId: String = ""
    private var categoryId: String = ""
    private var productId: String = ""

    private var isEditing = false
    private var itemPosition = -1
    private lateinit var orderRepository: OrderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            branchId = it.getString(ARG_BRANCH_ID, "")
            categoryId = it.getString(ARG_CATEGORY_ID, "")
            productId = it.getString(ARG_PRODUCT_ID, "")
            isEditing = it.getBoolean(ARG_IS_EDITING, false)
            itemPosition = it.getInt(ARG_ITEM_POSITION, -1)
        }

        val firestore = FirebaseFirestore.getInstance()
        favoriteRepository = FavoriteRepository(firestore)
        productRepository = ProductRepository(firestore)
        orderRepository = OrderRepository(firestore, requireContext())
        currentUserId = BeanieApplication.instance.getUserId()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFullscreenBottomSheet()

        // Hiển thị loading
        binding.loadingProgressBar.visibility = View.VISIBLE

        // Tải dữ liệu sản phẩm đầy đủ từ repository
        loadProductData()

        setupListeners()

        // Kiểm tra xem đang ở chế độ chỉnh sửa không
        if (isEditing) {
            binding.deleteButton.visibility = View.VISIBLE
            // Khôi phục thông tin size từ arguments
            val sizeId = arguments?.getString("size_id")
            val sizeName = arguments?.getString("size_name")
            val sizePrice = arguments?.getDouble("size_price", 0.0) ?: 0.0

            if (sizeId != null && sizeName != null) {
                selectedSize = ProductSize(sizeId, sizeName, sizePrice)
            }

            // Khôi phục thông tin toppings từ arguments
            val toppingIds = arguments?.getStringArray("topping_ids")
            val toppingNames = arguments?.getStringArray("topping_names")
            val toppingPrices = arguments?.getDoubleArray("topping_prices")

            if (toppingIds != null && toppingNames != null && toppingPrices != null) {
                selectedToppings.clear()
                for (i in toppingIds.indices) {
                    selectedToppings.add(
                        ProductTopping(
                            id = toppingIds[i],
                            name = toppingNames[i],
                            price = toppingPrices[i]
                        )
                    )
                }
            }

            // Khôi phục số lượng và ghi chú
            val initialQuantity = arguments?.getInt(ARG_INITIAL_QUANTITY, 1) ?: 1
            val initialNote = arguments?.getString(ARG_INITIAL_NOTE) ?: ""

            quantity = initialQuantity
            binding.quantityTextView.text = quantity.toString()
            binding.noteEditText.setText(initialNote)
        } else {
            binding.deleteButton.visibility = View.GONE
        }
    }

    private fun setupFullscreenBottomSheet() {
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun setupUI() {
        product?.let { product ->
            // Hiển thị ảnh sản phẩm
            if (product.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(product.imageUrl)
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.placeholder)
            }

            // Hiển thị thông tin cơ bản
            binding.productName.text = product.name
            binding.productPrice.text = "${product.price.toInt()}đ"
            binding.productDescription.text = product.description

            // Hiển thị số lượng
            binding.quantityTextView.text = quantity.toString()

            // Thiết lập phần size
            setupSizesOptions(product)

            // Thiết lập phần topping
            setupToppingsOptions(product)

            // Cập nhật tổng giá tiền
            updateTotalPrice()
        }
    }

    private fun loadProductData() {
        lifecycleScope.launch {
            try {
                binding.loadingProgressBar.visibility = View.VISIBLE

                // Log thông tin để debug
                android.util.Log.d("ProductDetail", "Loading product: productId=$productId, isEditing=$isEditing, position=$itemPosition")

                // Lấy thông tin sản phẩm
                product = if (isEditing && itemPosition >= 0) {
                    // Khi chỉnh sửa, lấy chi tiết sản phẩm trực tiếp từ collection products
                    val cartItems = orderRepository.getCartItems()
                    android.util.Log.d("ProductDetail", "Cart items: ${cartItems.size}")

                    if (itemPosition < cartItems.size) {
                        val cartItem = cartItems[itemPosition]
                        android.util.Log.d("ProductDetail", "Editing item: ${cartItem.productId}, ${cartItem.productName}")

                        // Sử dụng phương thức mới để lấy sản phẩm theo ID
                        productRepository.getProductById(cartItem.productId)
                    } else null
                } else {
                    // Trường hợp thêm mới, lấy sản phẩm từ chi tiết đầy đủ theo branch/category/product
                    productRepository.fetchCompleteProduct(branchId, categoryId, productId)
                }

                if (product != null) {
                    android.util.Log.d("ProductDetail", "Product loaded: ${product?.name}")
                    // Nếu lấy được thông tin sản phẩm, cập nhật UI
                    setupUI()

                    // Cập nhật button text cho phù hợp
                    if (isEditing) {
                        updateTotalPrice()
                        binding.addToCartBtn.text = "Cập nhật • ${totalPrice.toInt()}đ"
                    }
                } else {
                    android.util.Log.e("ProductDetail", "Product not found: $productId")
                    Toast.makeText(context, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetail", "Error loading product", e)
                Toast.makeText(context, "Lỗi khi tải sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            } finally {
                binding.loadingProgressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSizesOptions(product: Product) {
        binding.sizeRadioGroup.removeAllViews()

        // Ẩn phần size nếu không có size nào
        if (product.sizesAvailable.isEmpty()) {
            binding.sizeLabel?.visibility = View.GONE
            binding.sizeRadioGroup.visibility = View.GONE
            return
        }

        binding.sizeLabel?.visibility = View.VISIBLE
        binding.sizeRadioGroup.visibility = View.VISIBLE

        // Nếu đang sửa và chưa có selectedSize, thì cần gán size từ product
        if (!isEditing || selectedSize == null) {
            selectedSize = product.sizesAvailable.firstOrNull()
        }

        // Tìm index của size đã chọn (khi chỉnh sửa)
        val selectedSizeIndex = if (selectedSize != null) {
            product.sizesAvailable.indexOfFirst { it.id == selectedSize?.id }
        } else {
            0 // Mặc định chọn size đầu tiên
        }

        // Danh sách lưu các RadioButton để kiểm soát trạng thái
        val radioButtons = mutableListOf<RadioButton>()

        // Tạo UI cho mỗi size
        product.sizesAvailable.forEachIndexed { index, size ->
            val sizeLayout = layoutInflater.inflate(
                R.layout.item_size_option,
                binding.sizeRadioGroup,
                false
            ) as ViewGroup

            val radioButton = sizeLayout.findViewById<RadioButton>(R.id.sizeRadioButton)
            val sizeNameText = sizeLayout.findViewById<TextView>(R.id.sizeNameTextView)
            val sizePriceText = sizeLayout.findViewById<TextView>(R.id.sizePriceTextView)

            radioButton.id = View.generateViewId()
            sizeNameText.text = size.name
            sizePriceText.text = "${size.price.toInt()}đ"

            // Chọn size phù hợp
            radioButton.isChecked = index == selectedSizeIndex
            if (radioButton.isChecked) {
                selectedSize = size
            }

            // Thêm sự kiện click cho RadioButton
            radioButton.setOnClickListener {
                // Bỏ chọn tất cả các RadioButton khác
                radioButtons.forEach { rb -> rb.isChecked = false }
                // Chọn RadioButton hiện tại
                radioButton.isChecked = true
                // Cập nhật size được chọn
                selectedSize = size
                updateTotalPrice()
            }

            // Thêm vào danh sách để quản lý
            radioButtons.add(radioButton)

            binding.sizeRadioGroup.addView(sizeLayout)
        }
    }

    private fun setupToppingsOptions(product: Product) {
        // Xóa toàn bộ checkbox topping hiện tại
        val toppingContainer = binding.toppingContainer ?: return
        toppingContainer.removeAllViews()

        // Lấy danh sách topping ids từ sản phẩm
        val toppingIds = product.toppingsAvailable

        if (toppingIds.isEmpty()) {
            binding.toppingLabel?.visibility = View.GONE
            toppingContainer.visibility = View.GONE
            return
        }

        binding.toppingLabel?.visibility = View.VISIBLE
        toppingContainer.visibility = View.VISIBLE

        // Tải thông tin chi tiết topping từ repository
        lifecycleScope.launch {
            try {
                // Sử dụng hàm để lấy chi tiết toppings
                val toppings = productRepository.fetchProductToppings(branchId, product)

                if (toppings.isEmpty()) {
                    binding.toppingLabel?.visibility = View.GONE
                    toppingContainer.visibility = View.GONE
                    return@launch
                }

                // Danh sách id của toppings đã chọn
                val selectedToppingIds = selectedToppings.map { it.id }

                // Thêm các checkbox topping động
                for (topping in toppings) {
                    val toppingLayout = layoutInflater.inflate(
                        R.layout.item_topping_option,
                        toppingContainer,
                        false
                    )

                    val checkbox = toppingLayout.findViewById<CheckBox>(R.id.toppingCheckbox)
                    val nameTextView = toppingLayout.findViewById<TextView>(R.id.toppingNameTextView)
                    val priceTextView = toppingLayout.findViewById<TextView>(R.id.toppingPriceTextView)

                    nameTextView.text = topping.name
                    priceTextView.text = "${topping.price.toInt()}đ"

                    // Đánh dấu checkbox nếu topping này đã được chọn trước đó
                    checkbox.isChecked = selectedToppingIds.contains(topping.id)

                    checkbox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (!selectedToppings.any { it.id == topping.id }) {
                                selectedToppings.add(topping)
                            }
                        } else {
                            selectedToppings.removeAll { it.id == topping.id }
                        }
                        updateTotalPrice()
                    }

                    toppingContainer.addView(toppingLayout)
                }

                // Cập nhật tổng giá sau khi tải toppings
                updateTotalPrice()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải toppings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.favoriteButton.setOnClickListener {
            currentUserId?.let { uid ->
                isFavorite = !isFavorite
                updateFavoriteIcon(isFavorite)

                product?.let { product ->
                    if (isFavorite) {
                        favoriteRepository.addFavorite(uid, product)
                    } else {
                        favoriteRepository.removeFavorite(uid, product.id)
                    }
                }
            }
        }

        binding.decreaseButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityAndPrice()
            }
        }

        binding.increaseButton.setOnClickListener {
            quantity++
            updateQuantityAndPrice()
        }

        binding.addToCartBtn.setOnClickListener {
            addToCart()
        }

        binding.deleteButton.setOnClickListener {
            deleteCartItem()
        }
    }

    private fun deleteCartItem() {
        // Hiển thị dialog xác nhận trước khi xóa
        context?.let {
            android.app.AlertDialog.Builder(it)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?")
                .setPositiveButton("Xóa") { _, _ ->
                    // Thực hiện xóa sản phẩm
                    lifecycleScope.launch {
                        try {
                            if (isEditing && itemPosition >= 0) {
                                android.util.Log.d("ProductDetail", "Removing item at position $itemPosition")
                                val cartCount = orderRepository.removeFromCart(itemPosition)

                                // Thông báo về việc cập nhật giỏ hàng
                                listener?.onCartUpdated(cartCount)
                                Toast.makeText(context, "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ProductDetail", "Error removing cart item", e)
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        binding.favoriteButton.icon = ContextCompat.getDrawable(
            requireContext(),
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        )
    }

    private fun updateQuantityAndPrice() {
        binding.quantityTextView.text = quantity.toString()
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val basePrice = selectedSize?.price ?: product?.price ?: 0.0
        toppingPrice = selectedToppings.sumOf { it.price }
        totalPrice = (basePrice + toppingPrice) * quantity
        binding.addToCartBtn.text = if (isEditing) {
            "Cập nhật • ${totalPrice.toInt()}đ"
        } else {
            "Thêm • ${totalPrice.toInt()}đ"
        }
    }

    private fun addToCart() {
        val note = binding.noteEditText.text.toString().trim()

        // Kiểm tra sản phẩm không null
        product?.let { currentProduct ->
            // Khởi tạo OrderRepository đã được thực hiện trong onCreate

            // Thêm vào giỏ hàng trong coroutine
            lifecycleScope.launch {
                try {
                    val cartCount: Int

                    if (isEditing && itemPosition >= 0) {
                        // Cập nhật sản phẩm trong giỏ hàng
                        cartCount = orderRepository.updateCartItem(
                            position = itemPosition,
                            product = currentProduct,
                            selectedSize = selectedSize,
                            selectedToppings = selectedToppings,
                            quantity = quantity,
                            note = note
                        )

                        Toast.makeText(context, "Đã cập nhật sản phẩm trong giỏ hàng", Toast.LENGTH_SHORT).show()
                    } else {
                        // Thêm mới vào giỏ hàng
                        cartCount = orderRepository.addToCart(
                            product = currentProduct,
                            selectedSize = selectedSize,
                            selectedToppings = selectedToppings,
                            quantity = quantity,
                            note = note
                        )

                        // Hiển thị thông báo thành công
                        val toppingsText = selectedToppings.joinToString(", ") { it.name }
                        val message = """
                    Đã thêm ${currentProduct.name} vào giỏ hàng!
                    - Kích thước: ${selectedSize?.name ?: "Mặc định"}
                    - Topping: ${if (toppingsText.isEmpty()) "Không" else toppingsText}
                    - Số lượng: $quantity
                    - Ghi chú: ${if (note.isEmpty()) "Không" else note}
                    - Tổng giá: ${totalPrice.toInt()}đ
                    """.trimIndent()

                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }

                    // Thông báo về số lượng trong giỏ hàng
                    listener?.onCartUpdated(cartCount)
                    dismiss()

                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(context, "Không thể thêm sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface ProductDetailListener {
        fun onCartUpdated(itemCount: Int)
    }

    private var listener: ProductDetailListener? = null

    fun setProductDetailListener(listener: ProductDetailListener) {
        this.listener = listener
    }

    companion object {
        private const val ARG_BRANCH_ID = "branchId"
        private const val ARG_CATEGORY_ID = "categoryId"
        private const val ARG_PRODUCT_ID = "productId"
        private const val ARG_IS_EDITING = "isEditing"
        private const val ARG_ITEM_POSITION = "itemPosition"
        private const val ARG_INITIAL_QUANTITY = "initialQuantity"
        private const val ARG_INITIAL_NOTE = "initialNote"

        fun newInstance(
            branchId: String,
            categoryId: String,
            productId: String,
            isEditing: Boolean = false,
            itemPosition: Int = -1,
            initialSize: ProductSize? = null,
            initialToppings: List<ProductTopping> = listOf(),
            initialQuantity: Int = 1,
            initialNote: String = ""
        ): ProductDetailFragment {
            val fragment = ProductDetailFragment()
            val args = Bundle()
            args.putString(ARG_BRANCH_ID, branchId)
            args.putString(ARG_CATEGORY_ID, categoryId)
            args.putString(ARG_PRODUCT_ID, productId)
            args.putBoolean(ARG_IS_EDITING, isEditing)
            args.putInt(ARG_ITEM_POSITION, itemPosition)

            // Lưu thông tin size dưới dạng các thuộc tính riêng lẻ
            if (initialSize != null) {
                args.putString("size_id", initialSize.id)
                args.putString("size_name", initialSize.name)
                args.putDouble("size_price", initialSize.price)
            }

            // Lưu danh sách toppings dưới dạng danh sách các thuộc tính riêng lẻ
            if (initialToppings.isNotEmpty()) {
                val toppingIds = initialToppings.map { it.id }.toTypedArray()
                val toppingNames = initialToppings.map { it.name }.toTypedArray()
                val toppingPrices = initialToppings.map { it.price }.toDoubleArray()

                args.putStringArray("topping_ids", toppingIds)
                args.putStringArray("topping_names", toppingNames)
                args.putDoubleArray("topping_prices", toppingPrices)
            }

            args.putInt(ARG_INITIAL_QUANTITY, initialQuantity)
            args.putString(ARG_INITIAL_NOTE, initialNote)

            fragment.arguments = args
            return fragment
        }
    }
}