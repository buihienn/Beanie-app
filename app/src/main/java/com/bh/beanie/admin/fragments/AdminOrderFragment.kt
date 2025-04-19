package com.bh.beanie.admin.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminOrderAdapter
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.model.Order
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AdminOrderFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: AdminOrderAdapter
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_order, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewOrder)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        orderAdapter = AdminOrderAdapter(
            orderList = emptyList(), // tạm thời rỗng
            onConfirmClick = { order -> confirmOrder(order) },
            onCancelClick = { order -> cancelOrder(order) },
            onItemClick = { order -> viewOrderDetail(order) }
        )

        recyclerView.adapter = orderAdapter

        loadOrder() // Gọi hàm loadOrder() để fetch dữ liệu khi giao diện được tạo

        return view
    }

    private fun confirmOrder(order: Order) {
        // Cập nhật trạng thái đơn hàng khi xác nhận
//        lifecycleScope.launch {
//            try {
//                // Giả sử bạn sẽ cập nhật trạng thái đơn hàng thành "CONFIRMED"
//                repository.updateOrderStatus(order.id, "CONFIRMED")
//                // Sau khi xác nhận, bạn có thể làm gì đó như reload dữ liệu hoặc thông báo cho người dùng
//                loadOrder() // Tải lại danh sách đơn hàng
//            } catch (e: Exception) {
//                Log.e("AdminOrderFragment", "Lỗi khi xác nhận đơn hàng", e)
//            }
//        }
    }

    private fun cancelOrder(order: Order) {
        // Cập nhật trạng thái đơn hàng khi hủy
//        lifecycleScope.launch {
//            try {
//                // Giả sử bạn sẽ cập nhật trạng thái đơn hàng thành "CANCELED"
//                repository.updateOrderStatus(order.id, "CANCELED")
//                loadOrder() // Tải lại danh sách đơn hàng
//            } catch (e: Exception) {
//                Log.e("AdminOrderFragment", "Lỗi khi hủy đơn hàng", e)
//            }
//        }
    }

    private fun viewOrderDetail(order: Order) {
        // Xử lý khi người dùng nhấn vào một đơn hàng để xem chi tiết
        // Bạn có thể chuyển sang một Fragment mới hoặc mở một dialog, tùy theo yêu cầu của ứng dụng
        Log.d("AdminOrderFragment", "Xem chi tiết đơn hàng: ${order.id}")
    }

    private fun loadOrder() {
        lifecycleScope.launch {
            try {
                val orderList = repository.fetchAllOrdersWithItems() // Fetch tất cả đơn hàng cùng với các sản phẩm
                orderAdapter.updateOrders(orderList) // Cập nhật adapter với danh sách đơn hàng mới
            } catch (e: Exception) {
                Log.e("AdminOrderFragment", "Lỗi khi tải danh sách đơn hàng", e)
            }
        }
    }
}