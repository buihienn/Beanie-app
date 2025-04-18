package com.bh.beanie.admin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminVoucherAdapter
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminVoucherFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var voucherList: MutableList<Voucher> = mutableListOf()
    private lateinit var adapter: AdminVoucherAdapter
    private val firebaseRepository = FirebaseRepository(FirebaseFirestore.getInstance())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_admin_voucher, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerViewVoucher)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = AdminVoucherAdapter(voucherList)
        recyclerView.adapter = adapter

        fetchVouchers()

        val btnAddVoucher = rootView.findViewById<ImageButton>(R.id.imgBtnAddVoucher)

        return rootView
    }

    private fun fetchVouchers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val vouchers = firebaseRepository.fetchVouchersSuspend()
                voucherList.clear()
                voucherList.addAll(vouchers)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
