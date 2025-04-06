package com.bh.beanie.admin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.AdminCategoryAdapter
import com.bh.beanie.adapter.AdminCategoryItemAdapter
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem

class AdminInventoryFragment : Fragment() {
    private lateinit var recyclerViewCategory: RecyclerView
    private lateinit var categoryAdapter: AdminCategoryAdapter
    private val categories = mutableListOf<Category>()

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
            addNewItemToSelectedCategory()
        }

        return view
    }

    private fun setupRecyclerView() {
        categoryAdapter = AdminCategoryAdapter(categories,
            onCategoryClick = { category ->
                // Handle category click
            },
            onEditItemClick = { item ->
                showEditItemDialog(item)
            },
            onAddStockClick = { item ->
                addStockToItem(item)
            }
        )

        recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewCategory.adapter = categoryAdapter
    }

    private fun loadCategories() {
        // Sample data - in real app, load from database/API
        val coffeeItems = listOf(
            CategoryItem(
                id = "1",
                name = "Cappuccino",
                description = "Espresso with steamed milk",
                price = 3.5,
                imageUrl = "",
                stockQuantity = 50,
                categoryId = "1"
            ),
            CategoryItem(
                id = "2",
                name = "Espresso",
                description = "Strong black coffee",
                price = 2.5,
                imageUrl = "",
                stockQuantity = 75,
                categoryId = "1"
            )
        )

        val teaItems = listOf(
            CategoryItem(
                id = "3",
                name = "Trà sữa",
                description = "Trà sữa truyền thống",
                price = 4.0,
                imageUrl = "",
                stockQuantity = 100,
                categoryId = "2"
            )
        )

        categories.apply {
            add(Category(id = 1, name = "Cà phê", items = coffeeItems))
            add(Category(id = 2, name = "Trà", items = teaItems))
        }

        categoryAdapter.notifyDataSetChanged()
    }

    private fun addNewCategory() {
        // Create new empty category
        val newId = categories.maxOfOrNull { it.id }?.plus(1) ?: 1
        val newCategory = Category(
            id = newId,
            name = "Danh mục mới",
            items = emptyList()
        )

        categories.add(newCategory)
        categoryAdapter.notifyItemInserted(categories.size - 1)
    }

    private fun addNewItemToSelectedCategory() {
        // Implement logic to select category and add new item
    }

    private fun showEditItemDialog(item: CategoryItem) {
        // Show dialog to edit item details
    }

    private fun addStockToItem(item: CategoryItem) {
        // Implement stock addition logic
    }
}