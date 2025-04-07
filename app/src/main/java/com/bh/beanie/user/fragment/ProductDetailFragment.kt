package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentProductDetailBinding
import com.bh.beanie.user.model.Product
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProductDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: Product? = null
    private var quantity = 1
    private var selectedSize = "S"
    private var selectedSizePrice = 0.0
    private val selectedToppings = mutableListOf<String>()
    private var toppingPrice = 0.0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val productId = it.getInt(ARG_PRODUCT_ID)
            // Ở đây, bạn nên lấy dữ liệu sản phẩm từ repository hoặc view model
            product = Product(
                productId,
                it.getString(ARG_PRODUCT_NAME, ""),
                it.getDouble(ARG_PRODUCT_PRICE, 0.0),
                it.getInt(ARG_PRODUCT_IMAGE, R.drawable.matcha),
                it.getBoolean(ARG_PRODUCT_IS_FAVORITE, false)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo giá trị
        product?.let {
            selectedSizePrice = 45000.0 // Giá size S
            toppingPrice = 10000.0 // Giá cho White pearl
        }

        updateTotalPrice()

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        product?.let { product ->
            // Hiển thị thông tin sản phẩm
            binding.productImage.setImageResource(product.imageResourceId)
            binding.productName.text = product.name
            binding.productPrice.text = String.format("%.0fđ", product.price)
            binding.productDescription.text = "${product.name} description"

            // Hiển thị trạng thái yêu thích
            updateFavoriteIcon(product.isFavorite)

            // Mặc định chọn size S
            binding.sizeS.isChecked = true
            selectedSize = "S"
            selectedSizePrice = 45000.0 // Giá size S

            // Mặc định chọn White pearl
            binding.whitePearlCheckbox.isChecked = true
            selectedToppings.add("White pearl")
            toppingPrice = 10000.0

            updateTotalPrice()
        }
    }

    private fun setupListeners() {
        // Xử lý sự kiện đóng bottom sheet
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Xử lý sự kiện favorite
        binding.favoriteButton.setOnClickListener {
            product?.let {
                it.isFavorite = !it.isFavorite
                updateFavoriteIcon(it.isFavorite)
            }
        }

        // Xử lý sự kiện chọn size
        binding.sizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.sizeL -> {
                    selectedSize = "L"
                    selectedSizePrice = 55000.0
                }
                R.id.sizeM -> {
                    selectedSize = "M"
                    selectedSizePrice = 49000.0
                }
                R.id.sizeS -> {
                    selectedSize = "S"
                    selectedSizePrice = 45000.0
                }
            }
            updateTotalPrice()
        }

        // Xử lý sự kiện chọn topping
        setupToppingListeners()

        // Xử lý sự kiện tăng/giảm số lượng
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

        // Xử lý sự kiện thêm vào giỏ hàng
        binding.addToCartButton.setOnClickListener {
            addToCart()
        }
    }

    private fun setupToppingListeners() {
        binding.blackPearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedToppings.add("Black pearl")
                toppingPrice += 10000.0
            } else {
                selectedToppings.remove("Black pearl")
                toppingPrice -= 10000.0
            }
            updateTotalPrice()
        }

        binding.cheesePearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedToppings.add("Cheese pearl")
                toppingPrice += 10000.0
            } else {
                selectedToppings.remove("Cheese pearl")
                toppingPrice -= 10000.0
            }
            updateTotalPrice()
        }

        binding.whitePearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedToppings.add("White pearl")
                toppingPrice += 10000.0
            } else {
                selectedToppings.remove("White pearl")
                toppingPrice -= 10000.0
            }
            updateTotalPrice()
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
        totalPrice = (selectedSizePrice + toppingPrice) * quantity
        binding.addToCartButton.text = "Add • ${String.format("%.0fđ", totalPrice)}"
    }

    private fun addToCart() {
        // TODO: Thêm sản phẩm vào giỏ hàng
        // Trong ứng dụng thực tế, bạn nên sử dụng ViewModel hoặc Repository

        val note = binding.noteEditText.text.toString().trim()
        val toppingsText = selectedToppings.joinToString(", ")

        val cartItemInfo = """
            Added to cart:
            ${product?.name} (Size $selectedSize)
            Quantity: $quantity
            ${if (selectedToppings.isNotEmpty()) "Toppings: $toppingsText" else "No toppings"}
            ${if (note.isNotEmpty()) "Note: $note" else "No note"}
            Total: ${String.format("%.0fđ", totalPrice)}
        """.trimIndent()

        Toast.makeText(context, cartItemInfo, Toast.LENGTH_LONG).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"
        private const val ARG_PRODUCT_NAME = "product_name"
        private const val ARG_PRODUCT_PRICE = "product_price"
        private const val ARG_PRODUCT_IMAGE = "product_image"
        private const val ARG_PRODUCT_IS_FAVORITE = "product_is_favorite"

        fun newInstance(product: Product): ProductDetailFragment {
            return ProductDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PRODUCT_ID, product.id)
                    putString(ARG_PRODUCT_NAME, product.name)
                    putDouble(ARG_PRODUCT_PRICE, product.price)
                    putInt(ARG_PRODUCT_IMAGE, product.imageResourceId)
                    putBoolean(ARG_PRODUCT_IS_FAVORITE, product.isFavorite)
                }
            }
        }
    }
}