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
import com.bh.beanie.model.AdminOrder


class AdminOrderFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: AdminOrderAdapter

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

        val sampleOrders = listOf(
            AdminOrder("0123", "Nguyen Van A", "10/02/2025 - 09:30", "Pending", ""),
            AdminOrder("0456", "Tran Thi B", "11/02/2025 - 10:00", "Done", ""),
            AdminOrder("0789", "Le Van C", "12/02/2025 - 11:15", "Cancelled", "")
        )

        orderAdapter = AdminOrderAdapter(sampleOrders,
            onConfirmClick = { order -> confirmOrder(order) },
            onCancelClick = { order -> cancelOrder(order) }
        )

        recyclerView.adapter = orderAdapter

        return view
    }

    private fun confirmOrder(order: AdminOrder) {
        // TODO: Xử lý xác nhận đơn hàng
        println("Xác nhận đơn hàng: ${order.id}")
    }

    private fun cancelOrder(order: AdminOrder) {
        // TODO: Xử lý huỷ đơn hàng
        println("Huỷ đơn hàng: ${order.id}")
    }
}