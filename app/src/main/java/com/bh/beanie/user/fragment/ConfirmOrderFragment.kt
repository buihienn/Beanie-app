package com.bh.beanie.user.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Base64
import com.bh.beanie.R
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
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.error.ANError
import com.bh.beanie.user.UserOrderActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import org.json.JSONObject
import java.util.UUID

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

    private var orderId: String = ""
    private var paypalTransactionId: String = ""

    private var paymentMethod: String = "CASH"

    private val clientId: String = "AbagdadySoTpDKoqQxVJpDrlKCg3a3GepBDyIquKU7H9kmQL3uH56TY1Gt5mDz2zYISCatMHS8GujCgR"
    private val secretKey: String = "EHIzjXXKsuCEmrAjjVOf7nDM6qOKC9jx4vx7hlF2r9YNLBZiClYUlqaC5tkoe4cgzGMmWNgFwfkapBa3"
    private val returnUrl: String = "nativexo://paypalpay"
    private var accessToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidNetworking.initialize(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

            binding.orderModeTextView.text = "Delivery"

            binding.storeNameTextView.text = customerName
            binding.storeAddressTextView.text = deliveryAddress

            binding.deliveryField.visibility = View.VISIBLE
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
        binding.orderItemsTotalPrice.text = formattedPrice
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

        binding.paymentMethodLayout.setOnClickListener {
            showPaymentMethodSelector()
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

                    if (activity is UserOrderActivity) {
                        (activity as UserOrderActivity).updateCartCount(0)
                    }
                    dismiss()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmOrder(order: Order) {
        binding.progressBar.visibility = View.VISIBLE
        binding.confirmButton.isEnabled = false

        Log.d("ConfirmOrder", "payment method: $paymentMethod")
        fetchAccessToken()

//        // Kiểm tra phương thức thanh toán
//        if (paymentMethod == "PAYPAL") {
//            // Nếu là thanh toán PayPal, thực hiện quy trình PayPal
//            fetchAccessToken()
//        } else {
//            // Các phương thức thanh toán khác, xử lý đơn hàng trực tiếp
//            processOrder(order)
//        }
    }

    private fun processOrder(order: Order) {
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
                order.paymentMethod = paymentMethod
                order.transactionId = paypalTransactionId

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
                Log.e("OrderDetail", "Error creating order", e)
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

    private fun fetchAccessToken() {
        val authString = "$clientId:$secretKey"
        val encodedAuthString = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        AndroidNetworking.post("https://api-m.sandbox.paypal.com/v1/oauth2/token")
            .addHeaders("Authorization", "Basic $encodedAuthString")
            .addHeaders("Content-Type", "application/x-www-form-urlencoded")
            .addBodyParameter("grant_type", "client_credentials")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        accessToken = response.getString("access_token")
                        Log.d(TAG, "Token received: ${accessToken.take(10)}...")
                        startOrder()
                    } catch (e: Exception) {
                        handlePaymentError("Không thể xác thực với PayPal: ${e.message}")
                    }
                }

                override fun onError(error: ANError) {
                    Log.d(TAG, error.errorBody)
                }
            })
    }

    private fun handlePaymentError(message: String) {
        activity?.runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.confirmButton.isEnabled = true

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Lỗi thanh toán")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun handlerTransactionID(order: Order) {
        val config = CoreConfig(clientId, environment = Environment.SANDBOX)
        val payPalWebCheckoutClient = PayPalWebCheckoutClient(requireActivity(), config, returnUrl)

        val payPalWebCheckoutRequest =
            PayPalWebCheckoutRequest(paypalTransactionId, fundingSource = PayPalWebCheckoutFundingSource.PAYPAL)
        payPalWebCheckoutClient.start(payPalWebCheckoutRequest)
    }

    private fun startOrder() {
        try {
            // Tạo đơn hàng mới
            val order = Order()

            var totalAmountVND = 0.0
            // Tính tổng tiền bằng VND và chuyển sang USD
            lifecycleScope.launch {
                totalAmountVND = orderRepository.calculateTotal()
            }
            val totalAmountUSD = String.format("%.2f", totalAmountVND / 23000.0) // Tỷ giá ước tính

            orderId = UUID.randomUUID().toString()

            // Tạo JSON cho yêu cầu thanh toán PayPal
            val orderRequestJson = JSONObject().apply {
                put("intent", "CAPTURE")
                put("purchase_units", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("reference_id", orderId)
                        put("amount", JSONObject().apply {
                            put("currency_code", "USD")
                            put("value", totalAmountUSD)
                        })
                    })
                })
                put("payment_source", JSONObject().apply {
                    put("paypal", JSONObject().apply {
                        put("experience_context", JSONObject().apply {
                            put("payment_method_preference", "IMMEDIATE_PAYMENT_REQUIRED")
                            put("brand_name", "Beanie")
                            put("locale", "en-US")
                            put("landing_page", "LOGIN")
                            put("shipping_preference", "NO_SHIPPING")
                            put("user_action", "PAY_NOW")
                            put("return_url", returnUrl)
                            put("cancel_url", "https://example.com/cancelUrl")
                        })
                    })
                })
            }

            AndroidNetworking.post("https://api-m.sandbox.paypal.com/v2/checkout/orders")
                .addHeaders("Authorization", "Bearer $accessToken")
                .addHeaders("Content-Type", "application/json")
                .addHeaders("PayPal-Request-Id", orderId)
                .addJSONObjectBody(orderRequestJson)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        paypalTransactionId = response.getString("id")
                        Log.d(TAG, "PayPal Transaction ID: $paypalTransactionId")
                        handlerTransactionID(order)
                    }

                    override fun onError(error: ANError) {
                        Log.e(TAG, "Order Error: ${error.errorBody}")
                        handlePaymentError("Không thể tạo đơn hàng PayPal: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            handlePaymentError("Lỗi chuẩn bị đơn hàng: ${e.message}")
        }
    }

    private fun captureOrder(order: Order) {
        AndroidNetworking.post("https://api-m.sandbox.paypal.com/v2/checkout/orders/$paypalTransactionId/capture")
            .addHeaders("Authorization", "Bearer $accessToken")
            .addHeaders("Content-Type", "application/json")
            .addJSONObjectBody(JSONObject()) // Empty body
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d(TAG, "Capture Response: ${response.toString()}")
                    // lưu vào Firebase
                    processOrder(order)
                }

                override fun onError(error: ANError) {
                    Log.e(TAG, "Capture Error: ${error.errorDetail}")
                    handlePaymentError("Không thể hoàn tất thanh toán: ${error.message}")
                }
            })
    }

    fun handlePayPalResult(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.toString().startsWith(returnUrl)) {
            // User đã thanh toán hoặc cancel, kiểm tra kết quả
            val payerId = uri.getQueryParameter("PayerID")
            if (payerId != null) {
                Log.d(TAG, "PayPal Payment Approved with PayerID: $payerId")
                captureOrder(order) // Tiến hành capture đơn hàng
            } else {
                Log.e(TAG, "Payment cancelled or failed")
                handlePaymentError("Thanh toán bị huỷ hoặc thất bại.")
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

    private fun showPaymentMethodSelector() {
        val paymentMethodFragment = SelectPaymentMethodFragment.newInstance(paymentMethod)

        paymentMethodFragment.setPaymentMethodSelectedListener { method ->
            paymentMethod = method

            Log.d("Method", "$paymentMethod")

            // Cập nhật UI hiển thị phương thức thanh toán
            binding.paymentMethodTextView.text = paymentMethod
            binding.paymentIcon.visibility = View.VISIBLE

            // Cập nhật icon dựa vào phương thức
            val iconRes = when (method) {
                "CASH" -> R.drawable.ic_paypal
                "PAYPAL" -> R.drawable.ic_paypal
                "MOMO" -> R.drawable.ic_paypal
                "VNPAY" -> R.drawable.ic_paypal
                else -> R.drawable.ic_paypal
            }
            binding.paymentIcon.setBackgroundResource(iconRes)
        }

        paymentMethodFragment.show(parentFragmentManager, "paymentMethodSelector")
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
        const val TAG = "ConfirmOrderFragment"
        fun newInstance(): ConfirmOrderFragment {
            val fragment = ConfirmOrderFragment()
            return fragment
        }
    }
}