package com.bh.beanie.admin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminVoucherAdapter
import com.bh.beanie.model.AdminVoucher

class AdminVoucherFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var voucherList: List<AdminVoucher>
    private lateinit var adapter: AdminVoucherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_admin_voucher, container, false)

        // Khởi tạo RecyclerView
        recyclerView = rootView.findViewById(R.id.recyclerViewVoucher)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Tạo danh sách voucher mẫu
        voucherList = listOf(
            AdminVoucher("#V001", "Giam gia 50%", "10/02/2025", "Con hieu luc", ""),
            AdminVoucher("#V002", "Giam gia 30%", "20/05/2025", "Het han", ""),
            // Add more items
        )

        // Tạo adapter và thiết lập cho RecyclerView
        adapter = AdminVoucherAdapter(voucherList)
        recyclerView.adapter = adapter

        return rootView
    }
}