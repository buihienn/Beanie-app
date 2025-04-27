package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentOtherBinding
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.utils.NavigationUtils.logout
import kotlinx.coroutines.launch

class OtherFragment : Fragment() {
    private var _binding: FragmentOtherBinding? = null
    private val binding get() = _binding!!
    private lateinit var userRepository: UserRepository
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRepository = UserRepository()
        userId = BeanieApplication.instance.getUserId()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherBinding.inflate(inflater, container, false)

        if (userId.isNotEmpty()) {
            fetchUserDetails()
        }

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

        binding.redeem.setOnClickListener {
            val redeemFragment = RedeemFragment.newInstance()
            redeemFragment.show(parentFragmentManager, RedeemFragment.TAG)
        }
    }

    private fun showBranchSelectionDialog() {
        val selectBranchFragment = SelectBranchFragment.newInstance(true)
        selectBranchFragment.show(parentFragmentManager, "ViewBranchesFragment")
    }

    private fun fetchUserDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                user?.let {
                    // Cập nhật tên người dùng
                    binding.tvUserName.text = it.username ?: "User"

                    // Cập nhật điểm người dùng
                    binding.tvBeaniesCount.text = it.presentPoints.toString()

                    // Cập nhật loại thành viên nếu có
                    binding.tvMembershipType.text = it.membershipLevel ?: "Regular membership"
                }
            } catch (e: Exception) {
                Log.e("OtherFragment", "Error fetching user details: ${e.message}")
                // Hiển thị giá trị mặc định nếu có lỗi
                binding.tvBeaniesCount.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật thông tin người dùng mỗi khi fragment được hiển thị lại
        if (userId.isNotEmpty()) {
            fetchUserDetails()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = OtherFragment()
    }
}