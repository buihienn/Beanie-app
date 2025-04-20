package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bh.beanie.R
import com.bh.beanie.model.Product
import com.bh.beanie.repository.FavoriteRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailFragment : BottomSheetDialogFragment() {
    // View references
    private lateinit var productImageView: ImageView
    private lateinit var closeButton: ImageButton
    private lateinit var productNameTextView: TextView
    private lateinit var productPriceTextView: TextView
    private lateinit var productDescriptionTextView: TextView
    private lateinit var favoriteButton: MaterialButton
    private lateinit var sizeRadioGroup: RadioGroup
    private lateinit var sizeS: RadioButton
    private lateinit var sizeM: RadioButton
    private lateinit var sizeL: RadioButton
    private lateinit var blackPearlCheckbox: CheckBox
    private lateinit var cheesePearlCheckbox: CheckBox
    private lateinit var whitePearlCheckbox: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var decreaseButton: MaterialButton
    private lateinit var increaseButton: MaterialButton
    private lateinit var quantityTextView: TextView
    private lateinit var addToCartBtn: Button

    private var product: Product? = null
    private var quantity = 1
    private var selectedSize = "S"
    private var selectedSizePrice = 0.0
    private val selectedToppings = mutableListOf<String>()
    private var toppingPrice = 0.0
    private var totalPrice = 0.0
    private lateinit var favoriteRepository: FavoriteRepository
    private var currentUserId: String? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val productId = it.getString(ARG_PRODUCT_ID, "")
            val productName = it.getString(ARG_PRODUCT_NAME, "")
            val description = it.getString(ARG_PRODUCT_DESCRIPTION, "")
            val price = it.getDouble(ARG_PRODUCT_PRICE, 0.0)
            val imageUrl = it.getString(ARG_PRODUCT_IMAGE_URL, "")
            val stockQuantity = it.getInt(ARG_PRODUCT_STOCK_QUANTITY, 0)
            val categoryId = it.getString(ARG_PRODUCT_CATEGORY_ID, "")

            product = Product(
                productId,
                productName,
                description,
                price,
                imageUrl,
                stockQuantity,
                categoryId
            )
        }

        favoriteRepository = FavoriteRepository(FirebaseFirestore.getInstance())
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all view references
        initViews(view)
        setupFullscreenBottomSheet()

        product?.let {
            // Initialize with default values
            selectedSizePrice = it.price
            toppingPrice = 0.0
        }

        product?.let { product ->
            currentUserId?.let { userId ->
                favoriteRepository.isFavorite(userId, product.id) { isInFavorites ->
                    isFavorite = isInFavorites
                    updateFavoriteIcon(isFavorite)
                }
            }
        }

        setupUI()
        setupListeners()
    }

    private fun initViews(view: View) {
        productImageView = view.findViewById(R.id.productImage)
        closeButton = view.findViewById(R.id.closeButton)
        productNameTextView = view.findViewById(R.id.productName)
        productPriceTextView = view.findViewById(R.id.productPrice)
        productDescriptionTextView = view.findViewById(R.id.productDescription)
        favoriteButton = view.findViewById(R.id.favoriteButton)
        sizeRadioGroup = view.findViewById(R.id.sizeRadioGroup)
        sizeS = view.findViewById(R.id.sizeS)
        sizeM = view.findViewById(R.id.sizeM)
        sizeL = view.findViewById(R.id.sizeL)
        blackPearlCheckbox = view.findViewById(R.id.blackPearlCheckbox)
        cheesePearlCheckbox = view.findViewById(R.id.cheesePearlCheckbox)
        whitePearlCheckbox = view.findViewById(R.id.whitePearlCheckbox)
        noteEditText = view.findViewById(R.id.noteEditText)
        decreaseButton = view.findViewById(R.id.decreaseButton)
        increaseButton = view.findViewById(R.id.increaseButton)
        quantityTextView = view.findViewById(R.id.quantityTextView)
        addToCartBtn = view.findViewById(R.id.addToCartBtn)
    }

    private fun setupFullscreenBottomSheet() {
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.apply {
                peekHeight = resources.displayMetrics.heightPixels
                state = BottomSheetBehavior.STATE_EXPANDED
                isDraggable = true
            }
        }
    }

    private fun setupUI() {
        product?.let { product ->
            // Load image using Glide
            if (product.imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(product.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(productImageView)
            } else {
                productImageView.setImageResource(R.drawable.placeholder)
            }

            productNameTextView.text = product.name
            productPriceTextView.text = String.format("%.0fđ", product.price)
            productDescriptionTextView.text = product.description

            updateFavoriteIcon(isFavorite)

            sizeS.isChecked = true
            selectedSize = "S"
            selectedSizePrice = product.price

            // Reset topping selections
            whitePearlCheckbox.isChecked = false
            blackPearlCheckbox.isChecked = false
            cheesePearlCheckbox.isChecked = false
            selectedToppings.clear()
            toppingPrice = 0.0

            updateTotalPrice()
        }
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            dismiss()
        }

        favoriteButton.setOnClickListener {
            product?.let { product ->
                currentUserId?.let { userId ->
                    isFavorite = !isFavorite
                    if (isFavorite) {
                        favoriteRepository.addFavorite(userId, product)
                    } else {
                        favoriteRepository.removeFavorite(userId, product.id)
                    }
                    updateFavoriteIcon(isFavorite)
                }
            }
        }

        sizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            product?.let { product ->
                when (checkedId) {
                    R.id.sizeL -> {
                        selectedSize = "L"
                        selectedSizePrice = product.price + 10000.0 // +10k for L
                    }
                    R.id.sizeM -> {
                        selectedSize = "M"
                        selectedSizePrice = product.price + 4000.0 // +4k for M
                    }
                    R.id.sizeS -> {
                        selectedSize = "S"
                        selectedSizePrice = product.price // Base price for S
                    }
                }
                updateTotalPrice()
            }
        }

        setupToppingListeners()

        decreaseButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityAndPrice()
            }
        }

        increaseButton.setOnClickListener {
            quantity++
            updateQuantityAndPrice()
        }

        addToCartBtn.setOnClickListener {
            addToCart()
        }
    }

    private fun setupToppingListeners() {
        blackPearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedToppings.add("Black pearl")
                toppingPrice += 10000.0
            } else {
                selectedToppings.remove("Black pearl")
                toppingPrice -= 10000.0
            }
            updateTotalPrice()
        }

        cheesePearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedToppings.add("Cheese pearl")
                toppingPrice += 10000.0
            } else {
                selectedToppings.remove("Cheese pearl")
                toppingPrice -= 10000.0
            }
            updateTotalPrice()
        }

        whitePearlCheckbox.setOnCheckedChangeListener { _, isChecked ->
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
        favoriteButton.icon = ContextCompat.getDrawable(
            requireContext(),
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        )
    }

    private fun updateQuantityAndPrice() {
        quantityTextView.text = quantity.toString()
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        totalPrice = (selectedSizePrice + toppingPrice) * quantity
        addToCartBtn.text = "Add • ${String.format("%.0fđ", totalPrice)}"
    }

    private fun addToCart() {
        val note = noteEditText.text.toString().trim()
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

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"
        private const val ARG_PRODUCT_NAME = "product_name"
        private const val ARG_PRODUCT_DESCRIPTION = "product_description"
        private const val ARG_PRODUCT_PRICE = "product_price"
        private const val ARG_PRODUCT_IMAGE_URL = "product_image_url"
        private const val ARG_PRODUCT_STOCK_QUANTITY = "product_stock_quantity"
        private const val ARG_PRODUCT_CATEGORY_ID = "product_category_id"

        fun newInstance(product: Product): ProductDetailFragment {
            return ProductDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUCT_ID, product.id)
                    putString(ARG_PRODUCT_NAME, product.name)
                    putString(ARG_PRODUCT_DESCRIPTION, product.description)
                    putDouble(ARG_PRODUCT_PRICE, product.price)
                    putString(ARG_PRODUCT_IMAGE_URL, product.imageUrl)
                    putInt(ARG_PRODUCT_STOCK_QUANTITY, product.stockQuantity)
                    putString(ARG_PRODUCT_CATEGORY_ID, product.categoryId)
                }
            }
        }
    }
}