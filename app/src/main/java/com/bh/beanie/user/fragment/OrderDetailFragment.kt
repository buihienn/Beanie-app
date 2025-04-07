package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentOrderDetailBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OrderConfirmationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        Log.d("OrderConfirmation", "onCreateView executed")

        // Thiết lập sự kiện cho nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("OrderConfirmation", "onViewCreated executed")

        // Lấy bottom sheet từ dialog
        val bottomSheet: FrameLayout = dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!

        // Thiết lập chiều cao cho bottom sheet bằng với chiều cao màn hình
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        // Lấy behavior của bottom sheet và thiết lập
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            // Thiết lập peekHeight bằng chiều cao màn hình để hiển thị toàn màn hình ngay lập tức
            peekHeight = resources.displayMetrics.heightPixels
            // Mở rộng bottom sheet
            state = BottomSheetBehavior.STATE_EXPANDED

            // Thêm callback để theo dõi trạng thái của bottom sheet
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Có thể thêm xử lý khi trạng thái thay đổi nếu cần
                    Log.d("OrderConfirmation", "Bottom sheet state changed to: $newState")
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Có thể thêm xử lý khi bottom sheet trượt nếu cần
                    Log.d("OrderConfirmation", "Bottom sheet slide offset: $slideOffset")
                }
            })
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Các listener khác ngoài nút đóng
        binding.confirmButton.setOnClickListener {
            Toast.makeText(context, "Order confirmed!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.changeStoreButton.setOnClickListener {
            Toast.makeText(context, "Change store clicked", Toast.LENGTH_SHORT).show()
        }

        binding.timeLayout.setOnClickListener {
            Toast.makeText(context, "Choose time clicked", Toast.LENGTH_SHORT).show()
        }

        binding.addMoreButton.setOnClickListener {
            Toast.makeText(context, "Add more items clicked", Toast.LENGTH_SHORT).show()
        }

        binding.voucherLayout.setOnClickListener {
            Toast.makeText(context, "Choose voucher clicked", Toast.LENGTH_SHORT).show()
        }

        binding.paymentMethodLayout.setOnClickListener {
            Toast.makeText(context, "Choose payment method clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OrderConfirmationBottomSheet"

        fun newInstance(): OrderConfirmationBottomSheet {
            return OrderConfirmationBottomSheet()
        }
    }
}