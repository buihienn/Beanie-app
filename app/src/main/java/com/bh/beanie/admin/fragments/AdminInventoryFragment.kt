package com.bh.beanie.admin.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminCategoryAdapter
import com.bh.beanie.admin.dialogs.EditItemDialogFragment
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.admin.dialogs.AddItemDialogFragment

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
            addNewItem()
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
        lifecycleScope.launch {
            try {
                val fetchedCategories = repository.fetchCategoriesSuspend(branchId)
                categories.clear()
                categories.addAll(fetchedCategories)
                categoryAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("AdminInventoryFragment", "Error loading categories", e)
            }
        }
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
        lifecycleScope.launch {
            try {
                val items = repository.fetchCategoryItemsSuspend(branchId, categoryId)
                updateCategoryItems(categoryId, items)
            } catch (exception: Exception) {
                Log.e("AdminInventoryFragment", "Error fetching category items", exception)
            }
        }
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
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteCategoryItemSuspend(branchId, item.categoryId, item.id)
                        removeItemFromUI(item)
                        Toast.makeText(requireContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    } catch (exception: Exception) {
                        Log.e("AdminInventoryFragment", "Error deleting item", exception)
                        Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun removeItemFromUI(item: CategoryItem) {
        // Loại bỏ item khỏi danh sách và cập nhật lại RecyclerView
        val category = categories.find { it.id == item.categoryId }
        category?.let {
            val updatedItems = it.items.filterNot { it.id == item.id }
            val updatedCategory = it.copy(items = updatedItems)
            val categoryIndex = categories.indexOfFirst { it.id == updatedCategory.id }
            if (categoryIndex != -1) {
                categories[categoryIndex] = updatedCategory
                categoryAdapter.notifyItemChanged(categoryIndex)
            }
        }
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

    private fun addNewItem() {
        val dialog = AddItemDialogFragment(
            branchId = branchId,
            onItemAdded = { newItem ->
                // Tìm category tương ứng với item vừa thêm
                val categoryIndex = categories.indexOfFirst { it.id == newItem.categoryId }
                if (categoryIndex != -1) {
                    val category = categories[categoryIndex]
                    val updatedItems = category.items + newItem
                    categories[categoryIndex] = category.copy(items = updatedItems)
                    categoryAdapter.notifyItemChanged(categoryIndex)
                } else {
                    // Nếu category chưa có trong danh sách thì có thể thêm mới (nếu cần)
                    Toast.makeText(requireContext(), "Category not found", Toast.LENGTH_SHORT).show()
                }
            }
        )

        dialog.show(parentFragmentManager, "AddItemDialog")
    }
}