package com.bh.beanie.user.fragment

import android.os.Bundle
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
import com.bh.beanie.model.Order
import com.bh.beanie.repository.OrderRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderHistoryFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var emptyView: TextView
    private lateinit var orderRepository: OrderRepository
    private lateinit var backButton: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order_history, container, false)

        recyclerView = view.findViewById(R.id.orderHistoryRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.emptyView)

        // Khởi tạo Repository
        val db = FirebaseFirestore.getInstance()
        orderRepository = OrderRepository(db, requireContext())

        // Thiết lập RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = OrderAdapter(emptyList()) { order ->
            // Mở OrderDetailFragment khi người dùng nhấn vào đơn hàng
            openOrderDetail(order)
        }

        // Tải dữ liệu đơn hàng
        loadOrders()

        return view
    }

    private fun openOrderDetail(order: Order) {
        // Tạo instance của OrderDetailFragment với ID đơn hàng
        val orderDetailFragment = OrderDetailFragment.newInstance(order.id)

        // Thay thế fragment hiện tại bằng OrderDetailFragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, orderDetailFragment)
            .addToBackStack(null)  // Thêm vào back stack để có thể quay lại
            .commit()
    }

    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val orders = orderRepository.getUserOrders()

                if (orders.isEmpty()) {
                    // Hiển thị thông báo không có đơn hàng
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    // Cập nhật adapter với dữ liệu mới
                    (recyclerView.adapter as OrderAdapter).updateOrders(orders)
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            } catch (e: Exception) {
                // Xử lý lỗi
                progressBar.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Đã xảy ra lỗi: ${e.message}"
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance() = OrderHistoryFragment()
    }

    // Adapter cho RecyclerView
    private inner class OrderAdapter(
        private var orders: List<Order>,
        private val onOrderClick: (Order) -> Unit
    ) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

        fun updateOrders(newOrders: List<Order>) {
            orders = newOrders
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order_history, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            holder.bind(orders[position])
        }

        override fun getItemCount() = orders.size

        inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val orderIdTextView: TextView = itemView.findViewById(R.id.orderIdTextView)
            private val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
            private val orderTotalTextView: TextView = itemView.findViewById(R.id.orderTotalTextView)
            private val navigateButton: MaterialButton? = itemView.findViewById(R.id.navigateButton)

            init {
                // Sự kiện click vào item
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onOrderClick(orders[position])
                    }
                }

                // Sự kiện click vào nút chi tiết (nếu có)
                navigateButton?.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onOrderClick(orders[position])
                    }
                }
            }

            fun bind(order: Order) {
                try {
                    // Hiển thị ID đơn hàng (6 ký tự cuối)
                    val shortId = if (order.id.length > 6) order.id.takeLast(6) else order.id
                    orderIdTextView.text = "Mã đơn: #$shortId"

                    // Hiển thị ngày đặt hàng - xử lý trường hợp timestamp từ Firestore
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                    val dateStr = try {
                        dateFormat.format(order.orderTime.toDate())
                    } catch (e: Exception) {
                        "N/A"
                    }
                    orderDateTextView.text = dateStr

                    // Hiển thị tổng tiền
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    formatter.maximumFractionDigits = 0
                    val formattedPrice = formatter.format(order.totalPrice).replace("₫", "đ")
                    orderTotalTextView.text = formattedPrice
                } catch (e: Exception) {
                    // Xử lý lỗi binding - ghi log để debug
                    android.util.Log.e("OrderHistory", "Error binding order: ${e.message}")
                }
            }
        }
    }
}