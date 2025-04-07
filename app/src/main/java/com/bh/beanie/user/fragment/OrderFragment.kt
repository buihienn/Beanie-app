package com.bh.beanie.user.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bh.beanie.databinding.FragmentOrderBinding
import com.bh.beanie.user.UserOrderActivity
import com.bh.beanie.user.model.Address

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OrderFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
            // Mở trực tiếp Categories Activity
            startActivity(Intent(requireContext(), UserOrderActivity::class.java))
        }
    }

    private fun showAddressSelectionAndOpenCategories() {
        // Tạo và hiển thị bottom sheet chọn địa chỉ
        val selectAddressFragment = SelectAddressFragment.newInstance()

        // Thiết lập listener để biết khi nào địa chỉ được chọn
        selectAddressFragment.setAddressSelectedListener { address ->
            // Mở Categories Activity
            startActivity(Intent(requireContext(), UserOrderActivity::class.java))
        }

        selectAddressFragment.show(childFragmentManager, "addressSelector")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OrderFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}