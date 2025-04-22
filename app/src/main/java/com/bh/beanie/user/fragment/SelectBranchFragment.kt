package com.bh.beanie.user.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentSelectBranchBinding
import com.bh.beanie.databinding.ItemBranchBinding
import com.bh.beanie.model.Branch
import com.bh.beanie.repository.BranchRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SelectBranchFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectBranchBinding? = null
    private val binding get() = _binding!!

    private val branchList = mutableListOf<Branch>()
    private lateinit var branchAdapter: BranchAdapter
    private val branchRepository = BranchRepository()

    private var branchSelectedListener: ((Branch) -> Unit)? = null

    fun setBranchSelectedListener(listener: (Branch) -> Unit) {
        this.branchSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectBranchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập các thành phần UI
        setupUI()

        // Thiết lập RecyclerView
        setupRecyclerView()

        // Tải chi nhánh từ Firebase
        loadBranches()
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetStyle_Level1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)

            // Đặt chiều cao hiển thị là 90% màn hình
            val displayMetrics = requireContext().resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = (screenHeight * 0.9).toInt()
            bottomSheet.layoutParams = layoutParams

            behavior.peekHeight = (screenHeight * 0.9).toInt()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        return dialog
    }

    private fun setupUI() {
        // Thiết lập nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        branchAdapter = BranchAdapter(branchList)
        binding.branchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = branchAdapter
        }
    }

    private fun loadBranches() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        // Sử dụng lifecycleScope để gọi các hàm suspend
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val branches = branchRepository.fetchActiveBranches()
                binding.progressBar.visibility = View.GONE

                if (branches.isEmpty()) {
                    showEmptyState("Không có chi nhánh nào đang hoạt động")
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.branchRecyclerView.visibility = View.VISIBLE

                    branchList.clear()
                    branchList.addAll(branches)
                    branchAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyState("Lỗi khi tải chi nhánh: ${e.message}")
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.branchRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.emptyStateText.text = message
    }

    inner class BranchAdapter(private val branches: List<Branch>) :
        RecyclerView.Adapter<BranchAdapter.BranchViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchViewHolder {
            val binding = ItemBranchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return BranchViewHolder(binding)
        }

        override fun onBindViewHolder(holder: BranchViewHolder, position: Int) {
            val branch = branches[position]
            holder.bind(branch)
        }

        override fun getItemCount(): Int = branches.size

        inner class BranchViewHolder(private val binding: ItemBranchBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(branch: Branch) {
                binding.apply {
                    // Thiết lập tên chi nhánh
                    branchName.text = branch.name

                    // Thiết lập thông tin khoảng cách (có thể tính theo GPS trong tương lai)
                    distanceText.text = branch.location

                    // Thiết lập sự kiện xem chi tiết
                    detailButton.setOnClickListener {
                        Toast.makeText(requireContext(), "Xem chi tiết ${branch.name}", Toast.LENGTH_SHORT).show()
                    }

                    // Thiết lập sự kiện chọn chi nhánh
                    root.setOnClickListener {
                        saveSelectedBranch(branch)
                        branchSelectedListener?.invoke(branch)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun saveSelectedBranch(branch: Branch) {
        val sharedPreferences = requireActivity().getSharedPreferences("BeaniePref", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            // Lưu thông tin chi nhánh
            putString("selected_branch_id", branch.id)
            putString("selected_branch_name", branch.name)
            putString("selected_branch_location", branch.location)
            putString("selected_branch_phone", branch.phone)
            putString("selected_branch_image", branch.imageUrl)

            // Lưu chi nhánh dưới dạng String để hiển thị
            val displayText = "${branch.name}, ${branch.location}"
            putString("selected_branch_display", displayText)
        }
    }

    companion object {
        fun newInstance() = SelectBranchFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}