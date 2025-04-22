package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.databinding.FragmentOrderDetailBinding
import com.bh.beanie.model.OrderItem
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.user.adapter.CartItemAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class OrderDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var orderRepository: OrderRepository
    private var cartItems = listOf<OrderItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)

        // Khởi tạo OrderRepository
        orderRepository = OrderRepository(FirebaseFirestore.getInstance(), requireContext())

        // Thiết lập sự kiện cho nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập BottomSheet toàn màn hình
        setupFullscreenBottomSheet()

        // Tải dữ liệu giỏ hàng
        loadCartItems()

        // Thiết lập các listener
        setupListeners()
    }

    private fun setupFullscreenBottomSheet() {
        // Lấy bottom sheet từ dialog
        val bottomSheet: FrameLayout = dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!

        // Thiết lập chiều cao cho bottom sheet bằng với chiều cao màn hình
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        // Lấy behavior của bottom sheet và thiết lập
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            // Thiết lập peekHeight bằng chiều cao màn hình để hiển thị toàn màn hình ngay lập tức
            peekHeight = resources.displayMetrics.heightPixels
            // Mở rộng bottom sheet
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun updateCartUI(items: List<OrderItem>, totalPrice: Double) {
        // Định dạng tiền tệ
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0
        val formattedPrice = formatter.format(totalPrice).replace("₫", "đ")

        // Cập nhật tổng tiền
        binding.totalPriceTextView.text = formattedPrice
        binding.bottomBarTotalPrice.text = formattedPrice

        // Cập nhật số lượng sản phẩm
        binding.bottomBar.findViewById<android.widget.TextView>(com.bh.beanie.R.id.bottomBarTotalPrice)
            .text = "Take away • ${items.size} items"

        val adapter = CartItemAdapter(items) { item, position ->
            // Xử lý khi người dùng muốn sửa sản phẩm
            editCartItem(item, position)
        }

        binding.cartItemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cartItemsRecyclerView.adapter = adapter

        // Xóa các order item hiện tại (nếu có)
        // (Cần thêm container cho order items trong layout)

        // Thêm các sản phẩm vào UI (mẫu đã có sẵn trong XML)
        // Cách tốt hơn là sử dụng RecyclerView thay vì linear layout cố định
    }

    private fun loadCartItems() {
        lifecycleScope.launch {
            try {
                // Lấy danh sách sản phẩm từ giỏ hàng
                cartItems = orderRepository.getCartItems()

                // Log danh sách sản phẩm trong giỏ hàng
                android.util.Log.d("OrderDetail", "Cart items loaded: ${cartItems.size}")
                cartItems.forEachIndexed { index, item ->
                    android.util.Log.d("OrderDetail", "Item $index: ${item.productId}, ${item.productName}, size=${item.size?.name}, toppings=${item.toppings.size}")
                }

                // Tính tổng giá trị đơn hàng
                val totalPrice = orderRepository.calculateTotal()

                // Cập nhật UI với thông tin giỏ hàng
                updateCartUI(cartItems, totalPrice)
            } catch (e: Exception) {
                android.util.Log.e("OrderDetail", "Error loading cart items", e)
            }
        }
    }

    private fun editCartItem(item: OrderItem, position: Int) {
        // Log chi tiết về item được chỉnh sửa
        android.util.Log.d("OrderDetail", "Editing item at position $position:")
        android.util.Log.d("OrderDetail", "- productId: ${item.productId}")
        android.util.Log.d("OrderDetail", "- productName: ${item.productName}")
        android.util.Log.d("OrderDetail", "- size: ${item.size?.name ?: "None"}, ${item.size?.price ?: 0.0}đ")
        android.util.Log.d("OrderDetail", "- quantity: ${item.quantity}")
        android.util.Log.d("OrderDetail", "- toppings: ${item.toppings.size}")
        item.toppings.forEachIndexed { index, topping ->
            android.util.Log.d("OrderDetail", "  + Topping $index: ${topping.id}, ${topping.name}, ${topping.price}đ")
        }
        android.util.Log.d("OrderDetail", "- note: ${item.note}")

        // Hiển thị ProductDetailFragment để sửa sản phẩm
        val productDetailFragment = ProductDetailFragment.newInstance(
            branchId = arguments?.getString("branchId") ?: "",
            categoryId = "",
            productId = item.productId,
            isEditing = true,
            itemPosition = position,
            initialSize = item.size,
            initialToppings = item.toppings,
            initialQuantity = item.quantity,
            initialNote = item.note ?: ""
        )

        productDetailFragment.setProductDetailListener(object : ProductDetailFragment.ProductDetailListener {
            override fun onCartUpdated(itemCount: Int) {
                // Cập nhật lại giỏ hàng sau khi sửa
                android.util.Log.d("OrderDetail", "Cart updated, reloading items")
                loadCartItems()
            }
        })

        productDetailFragment.show(parentFragmentManager, "ProductDetailFragment")
    }

    private fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            confirmOrder()
        }

        // Các listener khác
        binding.changeStoreButton.setOnClickListener {
            // Hiển thị dialog chọn cửa hàng
        }

        binding.addMoreButton.setOnClickListener {
            // Quay lại màn hình sản phẩm
            dismiss()
        }
    }

    private fun confirmOrder() {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                binding.progressBar.visibility = View.VISIBLE
                binding.confirmButton.isEnabled = false

                // Get required information for the order
                val branchId = arguments?.getString("branchId") ?: ""
                // In a real app, these would come from user profile
                val customerName = "Khách hàng"
                val phoneNumber = ""

                // Create the order in Firebase
                val orderId = orderRepository.createOrder(
                    branchId = branchId,
                    customerName = customerName,
                    phoneNumber = phoneNumber,
                    deliveryAddress = "",
                    type = "TAKEAWAY", // or "DELIVERY" based on selection
                    paymentMethod = "CASH", // or selected payment method
                    note = "",
                )

                // Success - show confirmation
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.confirmButton.isEnabled = true

                    // Show success message
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Đặt hàng thành công")
                        .setMessage("Đơn hàng của bạn đã được gửi thành công. Mã đơn hàng: $orderId")
                        .setPositiveButton("OK") { _, _ ->
                            dismiss()
                            // You can navigate to order tracking screen here
                        }
                        .show()
                }
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("OrderDetail", "Error creating order", e)
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.confirmButton.isEnabled = true

                    // Show error message
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Lỗi đặt hàng")
                        .setMessage("Đã xảy ra lỗi: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }

    companion object {
        const val TAG = "OrderDetailFragment"

        fun newInstance(branchId: String): OrderDetailFragment {
            val fragment = OrderDetailFragment()
            val args = Bundle()
            args.putString("branchId", branchId)
            fragment.arguments = args
            return fragment
        }
    }
}