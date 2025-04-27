package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bh.beanie.databinding.FragmentSelectCategoryBinding
import com.bh.beanie.model.Category
import com.bh.beanie.repository.CategoryRepository
import com.bh.beanie.user.adapter.CategoryAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SelectCategoryFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryRepository: CategoryRepository
    private var onCategorySelectedListener: ((Category) -> Unit)? = null
    private var branchId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập RecyclerView
        binding.categoriesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
        }

        // Thiết lập nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        categoryRepository = CategoryRepository(FirebaseFirestore.getInstance())

        // Lấy branchId từ arguments
        branchId = arguments?.getString(ARG_BRANCH_ID) ?: ""

        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val (categories, _) = categoryRepository.fetchCategoriesPaginated(branchId)
                setupAdapter(categories)
            } catch (e: Exception) {
                // Xử lý lỗi
            }
        }
    }

    private fun setupAdapter(categories: List<Category>) {
        categoryAdapter = CategoryAdapter(requireContext(), categories, branchId) { category ->
            onCategorySelectedListener?.invoke(category)
            dismiss()
        }
        binding.categoriesRecyclerView.adapter = categoryAdapter
    }

    fun setOnCategorySelectedListener(listener: (Category) -> Unit) {
        onCategorySelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BRANCH_ID = "branch_id"

        fun newInstance(branchId: String): SelectCategoryFragment {
            val fragment = SelectCategoryFragment()
            val args = Bundle()
            args.putString(ARG_BRANCH_ID, branchId)
            fragment.arguments = args
            return fragment
        }
    }
}