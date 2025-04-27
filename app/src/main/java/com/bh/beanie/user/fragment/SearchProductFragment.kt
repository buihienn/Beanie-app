package com.bh.beanie.user.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.databinding.FragmentSearchProductBinding
import com.bh.beanie.model.Product
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.user.adapter.SearchResultAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchProductFragment : Fragment() {

    private var _binding: FragmentSearchProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var productRepository: ProductRepository
    private lateinit var searchResultAdapter: SearchResultAdapter
    private val searchResults = mutableListOf<Product>()
    private var searchJob: Job? = null

    private var branchId: String = ""
    private var onCartUpdateListener: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        branchId = arguments?.getString(ARG_BRANCH_ID) ?: ""

        productRepository = ProductRepository(FirebaseFirestore.getInstance())

        setupUI()
        setupSearch()
    }

    private fun setupUI() {
        // Thiết lập RecyclerView
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            searchResultAdapter = SearchResultAdapter(
                requireContext(),
                searchResults,
                branchId
            ) { itemCount ->
                onCartUpdateListener?.invoke(itemCount)
            }
            adapter = searchResultAdapter
        }

        // Thiết lập nút đóng
        binding.closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Hiển thị bàn phím khi fragment được mở
        binding.searchEditText.requestFocus()
    }

    private fun setupSearch() {
        // Xử lý sự kiện nhấn Enter trên bàn phím
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                performSearch(binding.searchEditText.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // Tìm kiếm theo thời gian thực khi người dùng nhập
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                val searchText = s.toString().trim()

                if (searchText.isEmpty()) {
                    // Xóa kết quả tìm kiếm nếu trống
                    searchResults.clear()
                    searchResultAdapter.notifyDataSetChanged()
                    binding.emptyResultsText.visibility = View.GONE
                    return
                }

                // Delay một chút để tránh tìm kiếm liên tục khi người dùng đang nhập
                searchJob = lifecycleScope.launch {
                    delay(300)
                    performSearch(searchText)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        if (query.trim().isEmpty()) return

        binding.searchProgressBar.visibility = View.VISIBLE
        binding.emptyResultsText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val results = productRepository.searchProducts(branchId, query)

                searchResults.clear()
                searchResults.addAll(results)
                searchResultAdapter.notifyDataSetChanged()

                // Hiển thị thông báo nếu không có kết quả
                if (results.isEmpty()) {
                    binding.emptyResultsText.visibility = View.VISIBLE
                    binding.searchResultsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyResultsText.visibility = View.GONE
                    binding.searchResultsRecyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.emptyResultsText.visibility = View.VISIBLE
                binding.searchResultsRecyclerView.visibility = View.GONE
            } finally {
                binding.searchProgressBar.visibility = View.GONE
            }
        }
    }

    fun setOnCartUpdateListener(listener: (Int) -> Unit) {
        onCartUpdateListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BRANCH_ID = "branch_id"

        fun newInstance(branchId: String): SearchProductFragment {
            val fragment = SearchProductFragment()
            val args = Bundle()
            args.putString(ARG_BRANCH_ID, branchId)
            fragment.arguments = args
            return fragment
        }
    }
}