package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bh.beanie.databinding.FragmentSelectPaymentMethodBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectPaymentMethodFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentSelectPaymentMethodBinding? = null
    private val binding get() = _binding!!

    private var paymentMethodSelectedListener: ((String) -> Unit)? = null
    private var currentPaymentMethod: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectPaymentMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập selected radio button dựa vào phương thức hiện tại
        setInitialSelectedPaymentMethod()

        // Thiết lập sự kiện khi nhấn nút xác nhận
        binding.confirmPaymentMethodButton.setOnClickListener {
            val paymentMethodId = binding.paymentMethodRadioGroup.checkedRadioButtonId
            val paymentMethod = when (paymentMethodId) {
                binding.cashPaymentRadio.id -> "CASH"
                binding.paypalPaymentRadio.id -> "PAYPAL"
                binding.momoPaymentRadio.id -> "MOMO"
                binding.vnpayPaymentRadio.id -> "VNPAY"
                else -> "CASH"
            }

            paymentMethodSelectedListener?.invoke(paymentMethod)
            dismiss()
        }

    }

    private fun setInitialSelectedPaymentMethod() {
        when (currentPaymentMethod) {
            "CASH" -> binding.cashPaymentRadio.isChecked = true
            "PAYPAL" -> binding.paypalPaymentRadio.isChecked = true
            "MOMO" -> binding.momoPaymentRadio.isChecked = true
            "VNPAY" -> binding.vnpayPaymentRadio.isChecked = true
            else -> binding.cashPaymentRadio.isChecked = true
        }
    }

    fun setPaymentMethodSelectedListener(listener: (String) -> Unit) {
        paymentMethodSelectedListener = listener
    }

    fun setCurrentPaymentMethod(method: String) {
        currentPaymentMethod = method
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(currentPaymentMethod: String = ""): SelectPaymentMethodFragment {
            val fragment = SelectPaymentMethodFragment()
            fragment.currentPaymentMethod = currentPaymentMethod
            return fragment
        }
    }
}