package com.bh.beanie.admin.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminCategoryAdapter
import com.bh.beanie.admin.dialogs.EditItemDialogFragment
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore

class AdminInventoryFragment : Fragment() {
    private lateinit var recyclerViewCategory: RecyclerView
    private lateinit var categoryAdapter: AdminCategoryAdapter
    private val categories = mutableListOf<Category>()
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())
    private val branchId = "braches_q5"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_inventory, container, false)

        // Initialize views
        recyclerViewCategory = view.findViewById(R.id.recyclerViewCategory)

        // Setup RecyclerView
        setupRecyclerView()

        // Load data
        loadCategories()

        // Setup button listeners
        view.findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            addNewCategory()
        }

        view.findViewById<Button>(R.id.btnAddItem).setOnClickListener {
//            addNewItemToSelectedCategory()
        }

        return view
    }

    private fun setupRecyclerView() {
        categoryAdapter = AdminCategoryAdapter(
            categories,
            onCategoryClick = { category ->
                fetchCategoryDetails(category.id)
            },
            onEditItemClick = { item ->
                showEditItemDialog(item)
            },
            onDeleteItemClick = { item ->
                deleteItem(item)
            }
        )

        recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewCategory.adapter = categoryAdapter
    }

    private fun loadCategories() {
        repository.fetchCategories(branchId,
            onSuccess = { fetchedCategories ->
                requireActivity().runOnUiThread {
                    categories.clear()
                    categories.addAll(fetchedCategories)
                    categoryAdapter.notifyDataSetChanged()
                }
            },
            onFailure = { exception ->
                Log.e("AdminInventoryFragment", "Error loading categories", exception)
            }
        )
    }

    private fun addNewCategory() {
        val newCategory = Category(
            id = System.currentTimeMillis().toString(), // Generate a unique ID
            name = "New Category",
            items = emptyList()
        )

        categories.add(newCategory)
        categoryAdapter.notifyItemInserted(categories.size - 1)
    }

    private fun fetchCategoryDetails(categoryId: String) {
        repository.fetchCategoryItems(branchId, categoryId,
            onSuccess = { items ->
                updateCategoryItems(categoryId, items)
            },
            onFailure = { exception ->
                Log.e("AdminInventoryFragment", "Error fetching category items", exception)
            }
        )
    }

    private fun showEditItemDialog(item: CategoryItem) {
        val dialog = EditItemDialogFragment(
            item = item,
            branchId = branchId,
            onItemUpdated = { updatedItem ->
                updateCategoryItemInUI(updatedItem)
            }
        )
        dialog.show(parentFragmentManager, "EditItemDialog")
    }

    private fun deleteItem(item: CategoryItem) {
        // Implement logic to add stock to the item
    }

    private fun updateCategoryItems(categoryId: String, items: List<CategoryItem>) {
        val categoryIndex = categories.indexOfFirst { it.id == categoryId }
        if (categoryIndex != -1) {
            val updatedCategory = categories[categoryIndex].copy(items = items)
            categories[categoryIndex] = updatedCategory
            categoryAdapter.notifyItemChanged(categoryIndex)
        }
    }

    private fun updateCategoryItemInUI(updatedItem: CategoryItem) {
        val categoryIndex = categories.indexOfFirst { it.id == updatedItem.categoryId }
        if (categoryIndex != -1) {
            val category = categories[categoryIndex]
            val itemIndex = category.items.indexOfFirst { it.id == updatedItem.id }
            if (itemIndex != -1) {
                val updatedItems = category.items.toMutableList()
                updatedItems[itemIndex] = updatedItem
                categories[categoryIndex] = category.copy(items = updatedItems)
                categoryAdapter.notifyItemChanged(categoryIndex)
            }
        }
    }
}