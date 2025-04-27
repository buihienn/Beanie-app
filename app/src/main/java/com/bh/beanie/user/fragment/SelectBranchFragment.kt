package com.bh.beanie.user.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentSelectBranchBinding
import com.bh.beanie.databinding.ItemBranchBinding
import com.bh.beanie.model.Branch
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.utils.BranchPreferences.clearBranch
import com.bh.beanie.utils.BranchPreferences.saveBranchId
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

    private var viewOnlyMode = false

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

        viewOnlyMode = arguments?.getBoolean("viewOnlyMode", false) ?: false

        if (viewOnlyMode) {
            binding.titleText.text = "Store"
        }

        // Thiết lập các thành phần UI
        setupUI()

        // Thiết lập RecyclerView
        setupRecyclerView()

        // Tải chi nhánh từ Firebase
        loadBranches()

        if (!viewOnlyMode) {
            clearBranch(requireContext())
        }
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
                        showBranchDetails(branch.id)
                    }

                    // Thiết lập sự kiện chọn chi nhánh
                    if (!viewOnlyMode) {
                        // Normal selection mode
                        root.setOnClickListener {
                            saveBranchId(requireContext(), branch.id)
                            branchSelectedListener?.invoke(branch)
                            dismiss()
                        }
                    } else {
                        // View-only mode: just show details instead of selecting
                        root.setOnClickListener {
                            showBranchDetails(branch.id)
                        }
                    }
                }
            }
        }
    }

    private fun showBranchDetails(branchId: String) {
        val detailFragment = if (!viewOnlyMode) {
            BranchDetailFragment.newInstance(branchId).apply {
                branchSelectedListener?.let { listener ->
                    setBranchSelectedListener(listener)
                }
            }
        } else {
            BranchDetailFragment.newInstance(branchId, viewOnlyMode)
        }

        detailFragment.show(parentFragmentManager, "BranchDetailFragment")
    }

    companion object {
        fun newInstance(viewOnlyMode: Boolean = false) = SelectBranchFragment().apply {
            arguments = Bundle().apply {
                putBoolean("viewOnlyMode", viewOnlyMode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}