package com.bh.beanie.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentOtherBinding
import com.bh.beanie.utils.NavigationUtils.logout

class OtherFragment : Fragment() {
    private var _binding: FragmentOtherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use view binding instead of findViewById
        _binding = FragmentOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardViewListeners()
    }

    private fun setupCardViewListeners() {
        // Profile card
        binding.cardProfile.setOnClickListener {
            // Navigate to EditProfile fragment
            val editProfileFragment = EditProfile.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        // Settings card
        binding.cardSettings.setOnClickListener {
            // Handle settings click
        }

        // Order History card
        binding.cardOrderHistory.setOnClickListener {
            val orderHistoryFragment = OrderHistoryFragment.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, orderHistoryFragment)
                .addToBackStack(null)
                .commit()
        }

        // Store card - Show branch selection dialog in view-only mode
        binding.cardStore.setOnClickListener {
            showBranchSelectionDialog()
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            activity?.let { currentActivity ->
                logout(currentActivity)
            }
        }
    }

    private fun showBranchSelectionDialog() {
        val selectBranchFragment = SelectBranchFragment.newInstance(true)
        selectBranchFragment.show(parentFragmentManager, "ViewBranchesFragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = OtherFragment()
    }
}