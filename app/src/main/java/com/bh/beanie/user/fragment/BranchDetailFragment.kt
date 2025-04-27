package com.bh.beanie.user.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentBranchDetailBinding
import com.bh.beanie.model.Branch
import com.bh.beanie.repository.BranchRepository
import com.bh.beanie.utils.BranchPreferences.saveBranchId
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class BranchDetailFragment : BottomSheetDialogFragment() {
    private var viewOnlyMode = false

    private var _binding: FragmentBranchDetailBinding? = null
    private val binding get() = _binding!!

    private val branchRepository = BranchRepository()
    private lateinit var branchId: String
    private var branch: Branch? = null

    private var branchSelectedListener: ((Branch) -> Unit)? = null

    fun setBranchSelectedListener(listener: (Branch) -> Unit) {
        this.branchSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBranchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get branch ID from arguments
        branchId = arguments?.getString(ARG_BRANCH_ID) ?: ""
        viewOnlyMode = arguments?.getBoolean(ARG_VIEW_ONLY, false) ?: false

        if (branchId.isEmpty()) {
            showError("Branch ID not provided")
            return
        }

        // Set up close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.selectBranchButton.visibility = if (viewOnlyMode) View.GONE else View.VISIBLE

        // Set up select branch button
        binding.selectBranchButton.setOnClickListener {
            branch?.let { branch ->
                saveBranchId(requireContext(), branch.id)
                branchSelectedListener?.invoke(branch)
                dismiss()
            }
        }

        // Load branch details
        loadBranchDetails()
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

    private fun loadBranchDetails() {
        showLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val branchDetail = branchRepository.fetchBranchById(branchId)
                if (branchDetail != null) {
                    branch = branchDetail
                    displayBranchDetails(branchDetail)
                } else {
                    showError("Branch not found")
                }
            } catch (e: Exception) {
                showError("Error loading branch: ${e.message}")
            }
        }
    }

    private fun displayBranchDetails(branch: Branch) {
        binding.apply {
            progressBar.visibility = View.GONE
            errorLayout.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE

            // Set branch name
            branchNameDetail.text = branch.name

            // Set location
            branchLocation.text = branch.location

            // Set phone
            branchPhone.text = branch.phone

            // Load image if available
            if (branch.imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(branch.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(branchImageView)
            } else {
                branchImageView.setImageResource(R.drawable.placeholder)
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.VISIBLE
            errorText.text = message
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BRANCH_ID = "branch_id"
        private const val ARG_VIEW_ONLY = "view_only_mode"

        fun newInstance(branchId: String, viewOnlyMode: Boolean = false): BranchDetailFragment {
            return BranchDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BRANCH_ID, branchId)
                    putBoolean(ARG_VIEW_ONLY, viewOnlyMode)
                }
            }
        }
    }
}