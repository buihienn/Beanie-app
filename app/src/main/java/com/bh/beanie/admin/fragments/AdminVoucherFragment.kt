package com.bh.beanie.admin.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.RadioGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminVoucherAdapter
import com.bh.beanie.admin.dialogs.AddVoucherDialogFragment
import com.bh.beanie.admin.dialogs.EditVoucherDialogFragment
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminVoucherFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var voucherList: MutableList<Voucher> = mutableListOf()
    private var originalVoucherList: MutableList<Voucher> = mutableListOf()
    private lateinit var adapter: AdminVoucherAdapter
    private lateinit var searchText: AutoCompleteTextView
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private lateinit var radioFilterGroup: RadioGroup

    private var selectedStateFilter: String = "" // "" tương đương với All
    private var currentSearchQuery: String = ""

    private val firebaseRepository = FirebaseRepository(FirebaseFirestore.getInstance())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_admin_voucher, container, false)

        // Initialize views
        searchText = rootView.findViewById(R.id.searchText)
        recyclerView = rootView.findViewById(R.id.recyclerViewVoucher)
        radioFilterGroup = rootView.findViewById(R.id.radioFilterGroupVoucher)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = AdminVoucherAdapter(voucherList) { voucher ->
            val editVoucherDialog = EditVoucherDialogFragment(voucher) { updatedVoucher ->
                val index = originalVoucherList.indexOfFirst { it.id == updatedVoucher.id }
                if (index != -1) {
                    originalVoucherList[index] = updatedVoucher
                    voucherList[index] = updatedVoucher
                    adapter.notifyItemChanged(index)
                    updateSuggestions()
                }
            }
            editVoucherDialog.show(parentFragmentManager, "EditVoucherDialog")
        }
        recyclerView.adapter = adapter

        // Setup AutoCompleteTextView
        autoCompleteAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        searchText.setAdapter(autoCompleteAdapter)

        // TextWatcher for filtering by content
        searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyCombinedFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // RadioGroup filtering by state
        radioFilterGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedStateFilter = when (checkedId) {
                R.id.radioFilterActive -> "ACTIVE"
                R.id.radioFilterDisabled -> "DISABLED"
                R.id.radioFilterExpired -> "EXPIRED"
                else -> "" // All
            }
            applyCombinedFilter()
        }

        // Add Voucher Button
        val btnAddVoucher = rootView.findViewById<ImageButton>(R.id.imgBtnAddVoucher)
        btnAddVoucher.setOnClickListener {
            val addVoucherDialog = AddVoucherDialogFragment(onVoucherAdded = { voucher ->
                originalVoucherList.add(0, voucher)
                applyCombinedFilter()
                updateSuggestions()
            })
            addVoucherDialog.show(parentFragmentManager, "AddVoucherDialog")
        }

        // Fetch vouchers from Firestore
        fetchVouchers()

        return rootView
    }

    private fun fetchVouchers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val vouchers = firebaseRepository.fetchVouchersSuspend()
                originalVoucherList.clear()
                originalVoucherList.addAll(vouchers)
                applyCombinedFilter()
                updateSuggestions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyCombinedFilter() {
        val filtered = originalVoucherList.filter { voucher ->
            val matchState = selectedStateFilter.isEmpty() || voucher.state == selectedStateFilter
            val matchSearch = currentSearchQuery.isBlank() || voucher.content.contains(currentSearchQuery, ignoreCase = true)
            matchState && matchSearch
        }
        voucherList.clear()
        voucherList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun updateSuggestions() {
        val contentSuggestions = originalVoucherList.map { it.content }.distinct()
        autoCompleteAdapter.clear()
        autoCompleteAdapter.addAll(contentSuggestions)
        autoCompleteAdapter.notifyDataSetChanged()
    }
}
