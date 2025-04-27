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
import com.bh.beanie.databinding.ItemOrderDetailLayoutBinding
import com.bh.beanie.model.Order
import com.bh.beanie.model.OrderItem
import com.bh.beanie.repository.OrderRepository
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

        setupRecyclerView()
        setupListeners()

        // Tải dữ liệu đơn hàng
        loadOrderDetails()
    }

    private fun setupRecyclerView() {
        binding.cartItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = OrderItemAdapter(emptyList())
        }
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.rateButton.setOnClickListener {
            // TODO: Mở dialog đánh giá đơn hàng
            Toast.makeText(context, "Đánh giá đơn hàng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrderDetails() {
        orderId?.let { id ->
            lifecycleScope.launch {
                try {
                    val order = orderRepository.getOrderById(id)
                    if (order != null) {
                        updateUI(order)
                    } else {
                        showErrorMessage("Không tìm thấy đơn hàng")
                    }
                } catch (e: Exception) {
                    showErrorMessage("Lỗi khi tải đơn hàng: ${e.message}")
                }
            }
        } ?: showErrorMessage("ID đơn hàng không hợp lệ")
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(order: Order) {
        // Tìm TextView trong layout để hiển thị loại đơn hàng
        val orderTypeView = binding.root.findViewById<TextView>(R.id.orderTypeTextView)

        // Cập nhật thông tin loại đơn hàng (TAKEAWAY hoặc DELIVERY)
        val orderType = if (order.type == "TAKEAWAY") "Take away" else "Delivery"
        orderTypeView?.text = orderType

        // Cập nhật thông tin cửa hàng
        binding.storeNameTextView.text = "Branch: ${order.branchId}"
        binding.storeAddressTextView.text = "Order time: ${formatDate(order.orderTime.toDate())}"

        // Cập nhật danh sách sản phẩm
        (binding.cartItemsRecyclerView.adapter as OrderItemAdapter).updateItems(order.items)

        // Cập nhật tổng tiền
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0
        val formattedPrice = formatter.format(order.totalPrice).replace("₫", "đ")
        binding.totalPriceTextView.text = formattedPrice

        // Cập nhật phương thức thanh toán
        binding.paymentMethodLayout.findViewById<TextView>(R.id.paymentMethodText).text =
            order.paymentMethod
    }

    private fun formatDate(date: java.util.Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        return dateFormat.format(date)
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
            val binding = ItemOrderDetailLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return OrderItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class OrderItemViewHolder(
            private val binding: ItemOrderDetailLayoutBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: OrderItem) {
                try {
                    // Hiển thị tên sản phẩm với kích thước nếu có
                    val displayName = if (item.size != null) {
                        "${item.productName} - ${item.size}"
                    } else {
                        item.productName
                    }
                    binding.productNameTextView.text = displayName
                    binding.quantityTextView.text = "x${item.quantity}"

                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    formatter.maximumFractionDigits = 0
                    val price = item.unitPrice * item.quantity
                    val formattedPrice = formatter.format(price).replace("₫", "đ")
                    binding.productPriceTextView.text = formattedPrice
                } catch (e: Exception) {
                    // Xử lý lỗi khi binding item
                    binding.productNameTextView.text = "Sản phẩm không xác định"
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