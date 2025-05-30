package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentOrderDetailBinding
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.repository.OrderRepository
import com.bh.beanie.dialog.RateOrderDialog
import com.bh.beanie.model.OrderRating
import com.bh.beanie.repository.RatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailFragment : Fragment() {
    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private var orderId: String? = null
    private lateinit var orderRepository: OrderRepository
    private lateinit var branchRepository: BranchRepository
    private lateinit var ratingRepository: RatingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderId = it.getString(ARG_ORDER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Repository
        val db = FirebaseFirestore.getInstance()
        orderRepository = OrderRepository(db, requireContext())
        branchRepository = BranchRepository(db)
        ratingRepository = RatingRepository()

        setupRecyclerView()
        setupListeners()

        // Tải dữ liệu đơn hàng
        loadOrderDetails()
    }

    private fun setupRecyclerView() {
        binding.cartItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = OrderItemAdapter(emptyList())
            visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Xử lý nút Hủy đơn
        binding.cancelButton.setOnClickListener {
            updateOrderStatus("CANCELLED")
        }

        // Xử lý nút Đã nhận
        binding.doneButton.setOnClickListener {
            updateOrderStatus("COMPLETED")
        }

        binding.rateButton.setOnClickListener {
            showRatingDialog()
        }
        checkOrderRating()
    }

    private fun loadOrderDetails() {
        orderId?.let { id ->
            lifecycleScope.launch {
                try {
                    val order = orderRepository.getOrderById(id)
                    if (order != null) {
                        Log.d("OrderDetail", "Order loaded: ${order.id}, items: ${order.items.size}")

                        updateUI(order)
                    } else {
                        showErrorMessage("Không tìm thấy đơn hàng")
                    }
                } catch (e: Exception) {
                    Log.e("OrderDetail", "Error loading order: ${e.message}", e)
                    showErrorMessage("Lỗi khi tải đơn hàng: ${e.message}")
                }
            }
        } ?: showErrorMessage("ID đơn hàng không hợp lệ")
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(order: Order) {
        // Update order ID
        binding.orderIdTextView.text = "Order ID: ${order.id}"

        // // Update order time
        val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        val formattedTime = sdf.format(order.orderTime.toDate())
        binding.timeOrderTextView.text = formattedTime

        // Update order status
        binding.orderStatusTextView.text = when (order.status) {
            "WAITING ACCEPT" -> "Your order is waiting acceptance"
            "READY FOR PICKUP" -> "Your order is ready for pickup"
            "PENDING" -> "Your order is pending"
            "DELIVERING" -> "Your order is being delivered"
            "COMPLETED" -> "Your order is completed"
            "CANCELLED" -> "Your order is cancelled"
            else -> "Unknown status"
        }

        updateButtonsVisibility(order.status)

        // Update store information
        if (order.type == "DELIVERY") {
            binding.orderTypeTextView.text = "Delivery"
            binding.storeNameTextView.text = "${order.customerName}"
            binding.storeAddressTextView.text = "${order.deliveryAddress}"
        } else {
            binding.orderTypeTextView.text = "Take away"

            lifecycleScope.launch {
                try {
                    val branch = branchRepository.fetchBranchById(order.branchId)
                    if (branch != null) {
                        Log.d("OrderDetail", "Branch loaded: ${branch.name}, location: ${branch.location}")
                        binding.storeNameTextView.text = branch.name
                        binding.storeAddressTextView.text = branch.location
                    } else {
                        Log.e("OrderDetail", "Branch not found for ID: ${order.branchId}")
                        binding.storeNameTextView.text = "Branch: ${order.branchId}"
                        binding.storeAddressTextView.text = "No location available"
                    }
                } catch (e: Exception) {
                    Log.e("OrderDetail", "Error fetching branch: ${e.message}")
                    binding.storeNameTextView.text = "Branch: ${order.branchId}"
                    binding.storeAddressTextView.text = "Error loading location"
                }
            }
        }

        Log.d("OrderDetail", "Items in order: ${order.items.size}")
        for (item in order.items) {
            Log.d("OrderDetail", "Item: ${item.productName}, Quantity: ${item.quantity}")
        }

        // Update items in the RecyclerView
        if (binding.cartItemsRecyclerView.adapter == null) {
            binding.cartItemsRecyclerView.adapter = OrderItemAdapter(order.items)
        } else {
            (binding.cartItemsRecyclerView.adapter as OrderItemAdapter).updateItems(order.items)
        }

        // Format currency
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0
        val formattedPrice = formatter.format(order.totalPrice).replace("₫", "đ")

        binding.totalPriceTextView.text = formattedPrice

        binding.paymentName.text = order.paymentMethod
    }

    private fun updateButtonsVisibility(status: String) {
        // Ẩn tất cả các nút trước
        binding.cancelButton.visibility = View.GONE
        binding.doneButton.visibility = View.GONE
        binding.rateButton.visibility = View.GONE

        // Hiển thị các nút tùy theo trạng thái
        when (status) {
            "WAITING ACCEPT" -> binding.cancelButton.visibility = View.VISIBLE
            "READY FOR PICKUP" -> binding.doneButton.visibility = View.VISIBLE
            "DELIVERING" -> binding.doneButton.visibility = View.VISIBLE
            "COMPLETED" -> binding.rateButton.visibility = View.VISIBLE
            // "PENDING" và "CANCELLED" không hiển thị nút nào
        }
    }

    private fun showRatingDialog() {
        orderId?.let { id ->
            lifecycleScope.launch {
                val existingRating = withContext(Dispatchers.IO) {
                    ratingRepository.getOrderRating(id)
                }

                RateOrderDialog(requireContext(), id, existingRating) { success ->
                    if (success) {
                        // Update the UI to show this order has been rated
                        binding.rateButton.text = "Update Rating"
                    }
                }.show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Order ID not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOrderRating() {
        orderId?.let { id ->
            lifecycleScope.launch {
                val existingRating = withContext(Dispatchers.IO) {
                    ratingRepository.getOrderRating(id)
                }

                if (existingRating != null) {
                    binding.rateButton.text = "Update Rating"
                } else {
                    binding.rateButton.text = "Rate"
                }
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        orderId?.let { id ->
            lifecycleScope.launch {
                try {
                    val success = orderRepository.updateOrderStatus(id, newStatus)
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "Đã cập nhật trạng thái đơn hàng thành $newStatus",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Tải lại thông tin đơn hàng sau khi cập nhật
                        loadOrderDetails()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Không thể cập nhật trạng thái đơn hàng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("OrderDetail", "Lỗi khi cập nhật trạng thái: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Lỗi: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Adapter cho RecyclerView sử dụng ViewBinding
    private inner class OrderItemAdapter(
        private var items: List<OrderItem>
    ) : RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder>() {

        fun updateItems(newItems: List<OrderItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
            // Thay đổi từ item_order_detail_layout sang cart_item_layout
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.cart_item_layout, parent, false)
            return OrderItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
            private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
            private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
            private val sizeTextView: TextView = itemView.findViewById(R.id.sizeTextView)
            private val toppingsTextView: TextView = itemView.findViewById(R.id.toppingsTextView)
            private val noteTextView: TextView = itemView.findViewById(R.id.noteTextView)
            private val editItemButton: View = itemView.findViewById(R.id.editItemButton)

            fun bind(item: OrderItem) {
                try {
                    // Ẩn nút edit
                    editItemButton.visibility = View.GONE

                    // Hiển thị tên sản phẩm
                    productNameTextView.text = item.productName

                    // Hiển thị số lượng
                    quantityTextView.text = "x${item.quantity}"

                    // Hiển thị size nếu có
                    if (item.size.isNotEmpty()) {
                        sizeTextView.text = item.size
                        sizeTextView.visibility = View.VISIBLE
                    } else {
                        sizeTextView.visibility = View.GONE
                    }

                    // Hiển thị toppings nếu có
                    if (item.toppings.isNotEmpty()) {
                        val toppingsText = item.toppings.joinToString(", ") { it.name }
                        toppingsTextView.text = toppingsText
                        toppingsTextView.visibility = View.VISIBLE
                    } else {
                        toppingsTextView.visibility = View.GONE
                    }

                    // Hiển thị ghi chú nếu có
                    if (item.note.isNotEmpty()) {
                        noteTextView.text = item.note
                        noteTextView.visibility = View.VISIBLE
                    } else {
                        noteTextView.visibility = View.GONE
                    }

                    // Hiển thị giá
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    formatter.maximumFractionDigits = 0
                    val price = item.unitPrice * item.quantity
                    val formattedPrice = formatter.format(price).replace("₫", "đ")
                    priceTextView.text = formattedPrice
                } catch (e: Exception) {
                    // Xử lý lỗi khi binding item
                    productNameTextView.text = "Sản phẩm không xác định"
                    Log.e("OrderDetail", "Error binding item: ${e.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "order_id"

        @JvmStatic
        fun newInstance(orderId: String) =
            OrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
    }
}