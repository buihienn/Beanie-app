package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.databinding.FragmentConfirmOrderBinding
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.user.adapter.CartItemAdapter
import com.bh.beanie.utils.BranchPreferences.getBranchId
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ConfirmOrderFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentConfirmOrderBinding? = null
    private val binding get() = _binding!!
    private lateinit var orderRepository: OrderRepository
    private var cartItems = listOf<OrderItem>()
    private var order: Order = Order()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmOrderBinding.inflate(inflater, container, false)

        // Khởi tạo OrderRepository
        orderRepository = OrderRepository(FirebaseFirestore.getInstance(), requireContext())

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
    }

    private fun loadCartItems() {
        lifecycleScope.launch {
            try {
                // Lấy danh sách sản phẩm từ giỏ hàng
                cartItems = orderRepository.getCartItems()

                // Log danh sách sản phẩm trong giỏ hàng
                android.util.Log.d("OrderDetail", "Cart items loaded: ${cartItems.size}")
                cartItems.forEachIndexed { index, item ->
                    android.util.Log.d("OrderDetail", "Item $index: ${item.productId}, ${item.productName}, size=${item.size}, toppings=${item.toppings.size}")
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
        Log.d("OrderDetail", "- categoryId: ${item.categoryId}")
        android.util.Log.d("OrderDetail", "- productName: ${item.productName}")
        android.util.Log.d("OrderDetail", "- size: ${item.size ?: "None"}, ${item.unitPrice ?: 0.0}đ")
        android.util.Log.d("OrderDetail", "- quantity: ${item.quantity}")
        android.util.Log.d("OrderDetail", "- toppings: ${item.toppings.size}")
        item.toppings.forEachIndexed { index, topping ->
            android.util.Log.d("OrderDetail", "  + Topping $index: ${topping.id}, ${topping.name}, ${topping.price}đ")
        }
        android.util.Log.d("OrderDetail", "- note: ${item.note}")

        // Hiển thị ProductDetailFragment để sửa sản phẩm
        val productDetailFragment = ProductDetailFragment.newInstance(
            branchId = getBranchId(requireContext()),
            categoryId = item.categoryId,
            productId = item.productId,
            isEditing = true,
            itemPosition = position,
            initialSize = Pair(item.size, item.unitPrice),
            initialToppings = item.toppings,
            initialQuantity = item.quantity,
            initialNote = item.note
        )
        Log.d("OrderDetail", "${getBranchId(requireContext())}, ${item.categoryId}, ${item.productId}")

        productDetailFragment.setProductDetailListener(object : ProductDetailFragment.ProductDetailListener {
            override fun onCartUpdated(itemCount: Int) {
                // Cập nhật lại giỏ hàng sau khi sửa
                Log.d("OrderDetail", "Cart updated, reloading items")
                loadCartItems()
            }
        })

        productDetailFragment.show(parentFragmentManager, "ProductDetailFragment")
    }

    private fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            confirmOrder(order)
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Các listener khác
        binding.changeStoreButton.setOnClickListener {
            // Hiển thị dialog chọn cửa hàng
        }

        binding.addMoreButton.setOnClickListener {
            // Quay lại màn hình sản phẩm
            dismiss()
        }

        binding.delButton.setOnClickListener {
            deleteOrder()
        }
    }

    private fun deleteOrder() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa giỏ hàng")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả sản phẩm trong giỏ hàng không?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    // Xóa giỏ hàng
                    orderRepository.clearCart()

                    // Cập nhật UI trước khi đóng fragment
                    cartItems = emptyList()
                    updateCartUI(cartItems, 0.0)

                    // Thông báo và đóng fragment
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Đã xóa tất cả sản phẩm trong giỏ hàng",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    if (activity is com.bh.beanie.user.UserOrderActivity) {
                        (activity as com.bh.beanie.user.UserOrderActivity).updateCartCount(0)
                    }
                    dismiss()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmOrder(order: Order) {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                binding.progressBar.visibility = View.VISIBLE
                binding.confirmButton.isEnabled = false

                val branchId = arguments?.getString("branchId") ?: ""

                val orderId = orderRepository.createOrder(order)

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
        fun newInstance(): ConfirmOrderFragment {
            val fragment = ConfirmOrderFragment()
            return fragment
        }
    }
}