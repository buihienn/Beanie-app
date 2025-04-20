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
import com.bh.beanie.adapter.AdminManageCustomerAdapter
import com.bh.beanie.admin.dialogs.EditCustomerDialogFragment
import com.bh.beanie.model.User
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AdminManageCustomer : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var customerAdapter: AdminManageCustomerAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchText: AutoCompleteTextView
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private lateinit var radioFilterGroupCustomer: RadioGroup

    private var originalCustomerList: MutableList<User> = mutableListOf()
    private var filteredCustomerList: MutableList<User> = mutableListOf()
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())

    private var isLoading = false
    private var isLastPage = false
    private var selectedGenderFilter: String = "" // "" means All
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_manage_customer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewCustomer)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        progressBar = view.findViewById(R.id.progressBarLoading)
        searchText = view.findViewById(R.id.searchText)
        radioFilterGroupCustomer = view.findViewById(R.id.radioFilterGroupCustomer)

        customerAdapter = AdminManageCustomerAdapter(
            customers = filteredCustomerList,
            onEditClick = { user -> editCustomer(user) }
        )
        recyclerView.adapter = customerAdapter

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

        radioFilterGroupCustomer.setOnCheckedChangeListener { _, checkedId ->
            selectedGenderFilter = when (checkedId) {
                R.id.radioFilterMale -> "male"
                R.id.radioFilterFemale -> "female"
                else -> "" // All
            }
            applyFilters()
        }

        loadCustomers()
    }

    private fun loadCustomers() {
        if (isLoading || isLastPage) return

        lifecycleScope.launch {
            isLoading = true
            progressBar.visibility = View.VISIBLE
            try {
                val (customers, lastVisible) = repository.fetchCustomersPaginated(lastVisibleDocument)

                if (customers.isEmpty()) {
                    isLastPage = true
                } else {
                    originalCustomerList.addAll(customers)
                    lastVisibleDocument = lastVisible
                    applyFilters()
                    updateSuggestions()
                }

            } catch (e: Exception) {
                Log.e("AdminManageCustomer", "Error loading customers", e)
            } finally {
                isLoading = false
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        val filtered = originalCustomerList.filter { customer ->
            val matchGender = selectedGenderFilter.isEmpty() || customer.gender.equals(selectedGenderFilter, ignoreCase = true)
            val matchSearch = currentSearchQuery.isBlank() || customer.username.contains(currentSearchQuery, ignoreCase = true)
            matchGender && matchSearch
        }
        filteredCustomerList.clear()
        filteredCustomerList.addAll(filtered)
        customerAdapter.notifyDataSetChanged()
    }

    private fun updateSuggestions() {
        val customerNames = originalCustomerList.map { it.username }.distinct()
        autoCompleteAdapter.clear()
        autoCompleteAdapter.addAll(customerNames)
        autoCompleteAdapter.notifyDataSetChanged()
    }

    private fun editCustomer(user: User) {
        val editCustomerDialog = EditCustomerDialogFragment(user) { updatedUser: User ->
            val index = originalCustomerList.indexOfFirst { it.username == updatedUser.username }
            if (index != -1) {
                originalCustomerList[index] = updatedUser
                applyFilters()
            }
        }
        editCustomerDialog.show(parentFragmentManager, "EditCustomerDialog")
    }
}