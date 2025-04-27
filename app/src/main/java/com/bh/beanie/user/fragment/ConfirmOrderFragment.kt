package com.bh.beanie.user.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.BeanieApplication
import com.bh.beanie.databinding.FragmentConfirmOrderBinding
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.user.adapter.CartItemAdapter
import com.bh.beanie.utils.BranchPreferences.getBranchId
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.text.format

class ConfirmOrderFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentConfirmOrderBinding? = null
    private val binding get() = _binding!!
    private lateinit var orderRepository: OrderRepository
    private lateinit var branchRepository: BranchRepository
    private lateinit var userRepository: UserRepository
    private var cartItems = listOf<OrderItem>()
    private var order: Order = Order()

    private var orderMode: String = ""
    private var branchId: String = ""
    private var customerName: String = ""
    private var phoneNumber: String = ""
    private var deliveryAddress: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmOrderBinding.inflate(inflater, container, false)

        // Khởi tạo OrderRepository
        orderRepository = OrderRepository(FirebaseFirestore.getInstance(), requireContext())
        branchRepository = BranchRepository(FirebaseFirestore.getInstance())
        userRepository = UserRepository()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập BottomSheet toàn màn hình
        setupFullscreenBottomSheet()

        // Tải dữ liệu giỏ hàng
        loadCartItems()

        // Lấy order mode
        val sharedPrefs = requireContext().getSharedPreferences("OrderMode", Context.MODE_PRIVATE)
        orderMode = sharedPrefs.getString("order_mode", "") ?: ""

        branchId = getBranchId(requireContext())

        loadUserAndStoreInfo()

        // Init time
        initTime()

        // Thiết lập các listener
        setupListeners()
    }

    private fun loadUserAndStoreInfo() {
        if (orderMode == "delivery") {
            // Lấy thông tin địa chỉ giao hàng
            val addressSharedPrefs = requireContext().getSharedPreferences("BeaniePref", Context.MODE_PRIVATE)
            customerName = addressSharedPrefs.getString("selected_customer_name", "") ?: ""
            phoneNumber = addressSharedPrefs.getString("selected_phone", "") ?: ""
            deliveryAddress = addressSharedPrefs.getString("selected_address_detail", "") ?: ""

            binding.storeNameTextView.text = customerName
            binding.storeAddressTextView.text = deliveryAddress
        } else {
            lifecycleScope.launch {
                try {
                    // Lấy thông tin người dùng
                    val user = userRepository.getCurrentUser()
                    if (user != null) {
                        customerName = user.username
                        phoneNumber = user.phone
                    }

                    // Lấy thông tin chi nhánh
                    val branch = branchRepository.fetchBranchById(branchId)
                    if (branch != null) {
                        activity?.runOnUiThread {
                            binding.storeNameTextView.text = branch.name
                            binding.storeAddressTextView.text = branch.location
                        }
                    } else {
                        Log.e("ConfirmOrder", "Branch not found with ID: $branchId")
                    }
                } catch (e: Exception) {
                    Log.e("ConfirmOrder", "Error fetching data: ${e.message}")
                }
            }
        }
    }

    private fun initTime() {
        // Khởi tạo giá trị mặc định cho ngày và giờ (thời gian hiện tại + 30 phút)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 30) // Thêm 30 phút
        selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        selectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        // Cập nhật UI thời gian
        updateTimeUI()
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
                Log.d("OrderDetail", "Cart items loaded: ${cartItems.size}")
                cartItems.forEachIndexed { index, item ->
                    android.util.Log.d("OrderDetail", "Item $index: ${item.productId}, ${item.productName}, size=${item.size}, toppings=${item.toppings.size}")
                }

                // Tính tổng giá trị đơn hàng
                val totalPrice = orderRepository.calculateTotal()

                // Cập nhật UI với thông tin giỏ hàng
                updateCartUI(cartItems, totalPrice)
            } catch (e: Exception) {
                Log.e("OrderDetail", "Error loading cart items", e)
            }
        }
    }

    private fun editCartItem(item: OrderItem, position: Int) {
        item.toppings.forEachIndexed { index, topping ->
            Log.d("OrderDetail", "  + Topping $index: ${topping.id}, ${topping.name}, ${topping.price}đ")
        }
        Log.d("OrderDetail", "- note: ${item.note}")

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

        binding.addMoreButton.setOnClickListener {
            dismiss()
        }

        binding.delButton.setOnClickListener {
            deleteOrder()
        }

        binding.storeInfoLayout.setOnClickListener {
            if (orderMode == "delivery") {
                showSelectAddressFragment()
            } else {
                showSelectBranchFragment()
            }
        }

        binding.timeLayout.setOnClickListener {
            if (orderMode == "delivery") {
                showDateTimePicker()
            } else {
                selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                showTimePicker()
            }
        }
    }

    private fun showDateTimePicker() {
        // Tạo Calendar với ngày giờ hiện tại
        val calendar = Calendar.getInstance()

        // Hiển thị DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Lưu ngày đã chọn
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Format ngày
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)

                // Tiếp tục hiển thị TimePicker sau khi chọn ngày
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Thiết lập ngày tối thiểu là ngày hiện tại
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        // Tính ngày tối đa (ví dụ: cho phép đặt trước 7 ngày)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_MONTH, 7)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        // Tạo Calendar với giờ hiện tại
        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minuteOfDay ->
                // Lưu giờ đã chọn
                selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfDay)

                // Cập nhật UI
                updateTimeUI()
            },
            hour,
            minute,
            true // 24h format
        )

        timePickerDialog.show()
    }

    private fun updateTimeUI() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        if (selectedDate == today) {
            binding.dateTextView.text = "Today"
        } else {
            binding.dateTextView.text = selectedDate
        }
        binding.timeTextView.text = selectedTime
    }

    private fun showSelectAddressFragment() {
        val selectAddressFragment = SelectAddressFragment.newInstance()

        selectAddressFragment.setAddressSelectedListener { address ->
            // Cập nhật địa chỉ giao hàng
            deliveryAddress = address.addressDetail
            customerName = address.name
            phoneNumber = address.phoneNumber

            // Cập nhật UI hiển thị địa chỉ mới
            binding.storeNameTextView.text = address.name
            binding.storeAddressTextView.text = address.addressDetail
        }

        selectAddressFragment.show(parentFragmentManager, "addressSelector")
    }

    private fun showSelectBranchFragment() {
        val selectBranchFragment = SelectBranchFragment.newInstance()

        selectBranchFragment.setBranchSelectedListener { branch ->
            // Cập nhật thông tin chi nhánh
            branchId = branch.id

            // Cập nhật UI hiển thị chi nhánh mới
            binding.storeNameTextView.text = branch.name
            binding.storeAddressTextView.text = branch.location
        }

        selectBranchFragment.show(parentFragmentManager, "branchSelector")
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

                order.userId = BeanieApplication.instance.getUserId()
                order.branchId = branchId
                order.customerName = customerName
                order.phoneNumber = phoneNumber
                order.orderTime = processTime(selectedDate, selectedTime)

                if (orderMode == "delivery") {
                    order.deliveryAddress = deliveryAddress
                    order.type = "DELIVERY"
                } else {
                    order.type = "TAKEAWAY"
                }

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

    fun processTime(selectedDate: String, selectedTime: String): Timestamp {
        // Kết hợp selectedDate và selectedTime thành một chuỗi datetime duy nhất
        val combinedDateTime = "$selectedDate $selectedTime" // Ví dụ: "26/04/2025 04:19"

        // Định dạng chuỗi ngày và giờ theo kiểu dd/MM/yyyy HH:mm
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Chuyển chuỗi thành Date
        val orderDate: Date? = dateTimeFormat.parse(combinedDateTime)

        // Nếu orderDate là null, dùng thời gian hiện tại
        return Timestamp(orderDate ?: Date())
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