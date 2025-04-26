package com.bh.beanie.user.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.bh.beanie.databinding.FragmentOrderBinding
import com.bh.beanie.user.UserOrderActivity

class OrderFragment : Fragment() {
    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập sự kiện cho card Delivery
        binding.deliveryCard.setOnClickListener {
            showAddressSelectionAndOpenCategories()
        }

        // Thiết lập sự kiện cho card Take Away
        binding.takeAwayCard.setOnClickListener {
            showBranchSelectionAndOpenCategories()
        }
    }

    private fun showAddressSelectionAndOpenCategories() {
        val selectAddressFragment = SelectAddressFragment.newInstance()

        selectAddressFragment.setAddressSelectedListener { address ->
            startActivity(Intent(requireContext(), UserOrderActivity::class.java).apply {
                putExtra("order_mode", "delivery")
                saveOrderMode("delivery")
            })
        }

        selectAddressFragment.show(childFragmentManager, "addressSelector")
    }

    private fun showBranchSelectionAndOpenCategories() {
        val selectBranchFragment = SelectBranchFragment.newInstance()

        selectBranchFragment.setBranchSelectedListener { branch ->
            startActivity(Intent(requireContext(), UserOrderActivity::class.java).apply {
                putExtra("order_mode", "take_away")
                saveOrderMode("take_away")
            })
        }

        selectBranchFragment.show(childFragmentManager, "branchSelector")
    }

    private fun saveOrderMode(orderMode: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("OrderMode", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            if (orderMode == "delivery") {
                putString("order_mode", "delivery")
            } else {
                putString("order_mode", "take_away")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = OrderFragment()
    }
}