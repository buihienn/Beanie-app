package com.bh.beanie.user.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.databinding.FragmentVoucherBinding
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.repository.UserVoucherRepository
import com.bh.beanie.repository.VoucherRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherFragment : DialogFragment() {

    private var _binding: FragmentVoucherBinding? = null
    private val binding get() = _binding!!

    private lateinit var voucherAdapter: VoucherAdapter
    private lateinit var voucherRepository: VoucherRepository
    private lateinit var userVoucherRepository: UserVoucherRepository
    private lateinit var userRepository: UserRepository

    // Chế độ hiển thị của fragment
    private var viewOnlyMode = false

    // Interface để lắng nghe sự kiện voucher được chọn
    interface OnVoucherSelectedListener {
        fun onVoucherSelected(voucher: Voucher)
    }

    private var voucherSelectedListener: OnVoucherSelectedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoucherBinding.inflate(inflater, container, false)

        // Thêm nút đóng ở header
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy chế độ từ arguments
        viewOnlyMode = arguments?.getBoolean("viewOnlyMode", false) ?: false

        // Initialize repositories
        voucherRepository = VoucherRepository()
        userVoucherRepository = UserVoucherRepository()
        userRepository = UserRepository()

        // Initialize RecyclerView
        binding.rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        voucherAdapter = VoucherAdapter(emptyList(), !viewOnlyMode) { voucher ->
            // Khi nhấp vào voucher, luôn hiển thị dialog chi tiết
            showVoucherDetailDialog(voucher)
        }
        binding.rvVouchers.adapter = voucherAdapter

        // Load vouchers
        loadVouchers()
    }

    private fun loadVouchers() {
        lifecycleScope.launch {
            try {
                Log.d("VoucherFragment", "Fetching user vouchers...")
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                if (currentUserId == null) {
                    Log.e("VoucherFragment", "User ID is null")
                    Toast.makeText(requireContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("VoucherFragment", "User ID: $currentUserId")
                val userVouchers = userVoucherRepository.getUserVouchers()

                Log.d("VoucherFragment", "Retrieved ${userVouchers.size} user vouchers")

                if (userVouchers.isNotEmpty()) {
                    // Extract just the Voucher objects from the pairs
                    val vouchers = userVouchers.map { it.second }
                    Log.d("VoucherFragment", "Updating adapter with ${vouchers.size} vouchers")
                    voucherAdapter.updateVouchers(vouchers)
                } else {
                    Log.d("VoucherFragment", "No vouchers found in the response")
                    Toast.makeText(requireContext(), "Hiện tại bạn chưa có voucher nào", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VoucherFragment", "Error loading vouchers", e)
                Log.e("VoucherFragment", "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(requireContext(), "Lỗi khi tải voucher: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showVoucherDetailDialog(voucher: Voucher) {
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_voucher_detail, null)

        // Find views
        val tvDialogDiscount = dialogView.findViewById<TextView>(R.id.tvDialogDiscount)
        val tvDialogMinOrder = dialogView.findViewById<TextView>(R.id.tvDialogMinOrder)
        val tvDialogExpiry = dialogView.findViewById<TextView>(R.id.tvDialogExpiry)
        val tvDialogDescription = dialogView.findViewById<TextView>(R.id.tvDialogDescription)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnUseVoucher = dialogView.findViewById<Button>(R.id.btnUseVoucher)

        // Set values
        tvDialogDiscount.text = when (voucher.discountType) {
            "PERCENTAGE" -> "${voucher.discountValue.toInt()}%"
            "FIXED" -> "${voucher.discountValue.toInt()}K"
            else -> "${voucher.discountValue.toInt()}"
        }

        tvDialogMinOrder.text = voucher.minOrderAmount.let {
            "${it.toInt()}K"
        }

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvDialogExpiry.text = voucher.expiryDate?.let {
            dateFormat.format(Date(it.seconds * 1000))
        } ?: "Không giới hạn"

        tvDialogDescription.text = voucher.content ?: "Không có mô tả"

        // Thiết lập hiển thị các nút dựa vào chế độ xem
        if (viewOnlyMode) {
            // Ở chế độ chỉ xem, ẩn nút Use Now, đổi text của nút Cancel thành "Đóng"
            btnUseVoucher.visibility = View.GONE
            btnCancel.text = "Đóng"
        } else {
            // Ở chế độ chọn, hiển thị cả hai nút
            btnUseVoucher.visibility = View.VISIBLE
        }

        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnUseVoucher.setOnClickListener {
            voucherSelectedListener?.onVoucherSelected(voucher)
            dialog.dismiss()
            // Đóng cả VoucherFragment khi đã chọn voucher
            if (!viewOnlyMode) {
                dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(viewOnlyMode: Boolean = false): VoucherFragment {
            return VoucherFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("viewOnlyMode", viewOnlyMode)
                }
            }
        }

        // Tạo instance với listener để chọn voucher
        fun newInstanceForSelection(listener: OnVoucherSelectedListener): VoucherFragment {
            return VoucherFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("viewOnlyMode", false)
                }
                voucherSelectedListener = listener
            }
        }
    }

    inner class VoucherAdapter(
        private var vouchers: List<Voucher>,
        private val showSelectButton: Boolean,
        private val onVoucherClick: (Voucher) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>() {

        fun updateVouchers(newVouchers: List<Voucher>) {
            this.vouchers = newVouchers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher, parent, false)
            return VoucherViewHolder(view)
        }

        override fun getItemCount() = vouchers.size

        override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
            holder.bind(vouchers[position])
        }

        inner class VoucherViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val tvDiscountAmount: TextView = itemView.findViewById(R.id.tvDiscountAmount)
            private val tvVoucherContent: TextView = itemView.findViewById(R.id.tvVoucherContent)
            private val imgClock: ImageView = itemView.findViewById(R.id.imgClock)
            private val tvTimeLabel: TextView = itemView.findViewById(R.id.tvTimeLabel)
            private val tvExpireDate: TextView = itemView.findViewById(R.id.tvExpireDate)
            private val btnSelect: Button = itemView.findViewById(R.id.btnSelect)

            fun bind(voucher: Voucher) {
                // Format discount amount
                tvDiscountAmount.text = when (voucher.discountType) {
                    "PERCENTAGE" -> "${voucher.discountValue.toInt()}%"
                    "FIXED" -> "${voucher.discountValue.toInt()}k"
                    else -> "${voucher.discountValue.toInt()}"
                }

                // Set voucher content
                val minOrderText = voucher.minOrderAmount.let {
                    "FROM ${it.toInt()}K"
                }

                tvVoucherContent.text = "${getDiscountText(voucher)} OFF $minOrderText"

                // Set expiration date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                voucher.expiryDate?.let { expiryDate ->
                    try {
                        val date = Date(expiryDate.seconds * 1000)
                        tvExpireDate.text = dateFormat.format(date)
                    } catch (e: Exception) {
                        tvExpireDate.text = "Ngày không hợp lệ"
                        Log.e("VoucherFragment", "Error formatting date", e)
                    }
                } ?: run {
                    tvExpireDate.text = "Không giới hạn"
                }

                // Chỉ hiển thị nút Select khi không ở chế độ chỉ xem
                btnSelect.visibility = if (showSelectButton) View.VISIBLE else View.GONE

                // Thiết lập click listener cho cả item
                itemView.setOnClickListener {
                    onVoucherClick(voucher)
                }

                // Nút Select sẽ áp dụng voucher và đóng dialog
                btnSelect.setOnClickListener {
                    voucherSelectedListener?.onVoucherSelected(voucher)
                    // Đóng cả fragment khi đã chọn voucher
                    if (!viewOnlyMode) {
                        dismiss()
                    }
                }
            }

            private fun getDiscountText(voucher: Voucher): String {
                return when (voucher.discountType) {
                    "PERCENTAGE" -> "${voucher.discountValue.toInt()}%"
                    "FIXED" -> "${voucher.discountValue.toInt()}K"
                    else -> "${voucher.discountValue.toInt()}"
                }
            }
        }
    }
}