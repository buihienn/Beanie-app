package com.bh.beanie.admin.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminOrderAdapter
import com.bh.beanie.model.Order
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AdminOrderFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: AdminOrderAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchText: AutoCompleteTextView
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private lateinit var radioFilterGroupRow1: RadioGroup
    private lateinit var radioFilterGroupRow2: RadioGroup

    private lateinit var radioGroupRow1Listener: RadioGroup.OnCheckedChangeListener
    private lateinit var radioGroupRow2Listener: RadioGroup.OnCheckedChangeListener

    private var originalOrderList: MutableList<Order> = mutableListOf()
    private var filteredOrderList: MutableList<Order> = mutableListOf()
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())

    private var isLoading = false
    private var isLastPage = false
    private var selectedStatusFilter: String = "" // "" means All
    private var currentSearchQuery: String = ""
    private var branchId : String = arguments?.getString("branchId") ?: "braches_q5"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_order, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewOrder)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        progressBar = view.findViewById(R.id.progressBarLoading)
        searchText = view.findViewById(R.id.searchText)
        radioFilterGroupRow1 = view.findViewById(R.id.radioGroupRow1)
        radioFilterGroupRow2 = view.findViewById(R.id.radioGroupRow2)

        orderAdapter = AdminOrderAdapter(
            orderList = filteredOrderList,
            onConfirmClick = { order -> updateOrderStatus(order.id, "PENDING") },
            onCancelClick = { order -> updateOrderStatus(order.id, "CANCELED") },
            onCompleteClick = { order -> completeOrder(order.id, order.type) },
            onItemClick = { order -> viewOrderDetail(order) }
        )
        recyclerView.adapter = orderAdapter

        autoCompleteAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        searchText.setAdapter(autoCompleteAdapter)

        searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // RadioGroup listeners setup
        radioGroupRow1Listener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                radioFilterGroupRow2.setOnCheckedChangeListener(null)
                radioFilterGroupRow2.clearCheck()
                radioFilterGroupRow2.setOnCheckedChangeListener(radioGroupRow2Listener)

                selectedStatusFilter = when (checkedId) {
                    R.id.radioBtnCompleted -> "COMPLETED"
                    R.id.radioBtnPending -> "PENDING"
                    R.id.radioBtnCancel -> "CANCELED"
                    else -> "" // All
                }
                applyFilters()
            }
        }

        radioGroupRow2Listener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                radioFilterGroupRow1.setOnCheckedChangeListener(null)
                radioFilterGroupRow1.clearCheck()
                radioFilterGroupRow1.setOnCheckedChangeListener(radioGroupRow1Listener)

                selectedStatusFilter = when (checkedId) {
                    R.id.radioBtnPickup -> "READY FOR PICKUP"
                    R.id.radioBtnDelivering -> "DELIVERING"
                    R.id.radioBtnWaiting -> "WAITING ACCEPT"
                    else -> "" // All
                }
                applyFilters()
            }
        }

        radioFilterGroupRow1.setOnCheckedChangeListener(radioGroupRow1Listener)
        radioFilterGroupRow2.setOnCheckedChangeListener(radioGroupRow2Listener)

        loadOrders()

        return view
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            isLoading = true
            progressBar.visibility = View.VISIBLE
            try {
                val (orders, lastVisible) = repository.fetchOrdersPaginated(branchId,lastVisibleDocument)

                if (orders.isEmpty()) {
                    isLastPage = true
                } else {
                    originalOrderList.addAll(orders)
                    lastVisibleDocument = lastVisible
                    applyFilters()
                    updateSuggestions()
                }

            } catch (e: Exception) {
                Log.e("AdminOrderFragment", "Error loading orders", e)
            } finally {
                isLoading = false
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        val filtered = originalOrderList.filter { order ->
            val matchStatus = selectedStatusFilter.isEmpty() || order.status == selectedStatusFilter
            val matchSearch = currentSearchQuery.isBlank() || order.customerName.contains(currentSearchQuery, ignoreCase = true)
            matchStatus && matchSearch
        }
        filteredOrderList.clear()
        filteredOrderList.addAll(filtered)
        orderAdapter.notifyDataSetChanged()
    }

    private fun updateSuggestions() {
        val customerNames = originalOrderList.map { it.customerName }.distinct()
        autoCompleteAdapter.clear()
        autoCompleteAdapter.addAll(customerNames)
        autoCompleteAdapter.notifyDataSetChanged()
    }

    private fun updateOrderStatus(orderId: String, status: String) {
        lifecycleScope.launch {
            try {
                repository.updateOrderStatus(orderId, status)
                refreshOrders()
            } catch (e: Exception) {
                Log.e("AdminOrder", "Error updating order status: ${e.message}")
            }
        }
    }

    private fun completeOrder(orderId: String, typeOrder: String) {
        val status = if (typeOrder == "TAKEAWAY") "READY FOR PICKUP" else "DELIVERING"
        lifecycleScope.launch {
            try {
                repository.updateOrderStatus(orderId, status)
                refreshOrders()
            } catch (e: Exception) {
                Log.e("AdminOrder", "Error completing order: ${e.message}")
            }
        }
    }

    private fun refreshOrders() {
        filteredOrderList.clear()
        originalOrderList.clear()
        lastVisibleDocument = null
        isLastPage = false
        loadOrders()
    }

    private fun viewOrderDetail(order: Order) {
        Log.d("AdminOrderFragment", "Viewing order details: ${order.id}")
        // Open a Fragment or Dialog to display order details
    }
}
