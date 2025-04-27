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
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentRedeemBinding
import com.bh.beanie.model.User
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.repository.UserVoucherRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RedeemFragment : DialogFragment() {

    private var _binding: FragmentRedeemBinding? = null
    private val binding get() = _binding!!

    private lateinit var redeemAdapter: RedeemAdapter
    private lateinit var userVoucherRepository: UserVoucherRepository
    private lateinit var userRepository: UserRepository
    private var currentUser: User? = null

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
        _binding = FragmentRedeemBinding.inflate(inflater, container, false)

        // Thêm nút đóng ở header
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repositories
        userVoucherRepository = UserVoucherRepository()
        userRepository = UserRepository()

        // Initialize RecyclerView
        binding.rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        redeemAdapter = RedeemAdapter(emptyList()) { voucher ->
            showRedeemConfirmDialog(voucher)
        }
        binding.rvVouchers.adapter = redeemAdapter

        // Load data
        loadCurrentUser()
        loadRedeemableVouchers()
    }

    private fun loadCurrentUser() {
        lifecycleScope.launch {
            try {
                currentUser = userRepository.getCurrentUser()
                currentUser?.let {
                    binding.tvUserPoints.text = "Điểm hiện tại: ${it.presentPoints}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user", e)
                Toast.makeText(requireContext(), "Lỗi khi tải thông tin người dùng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRedeemableVouchers() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching redeemable vouchers...")
                val redeemableVouchers = userVoucherRepository.getActiveVouchersForRedeem()

                if (redeemableVouchers.isNotEmpty()) {
                    Log.d(TAG, "Retrieved ${redeemableVouchers.size} redeemable vouchers")
                    redeemAdapter.updateVouchers(redeemableVouchers)
                } else {
                    Log.d(TAG, "No redeemable vouchers found")
                    Toast.makeText(requireContext(), "Hiện không có voucher nào để đổi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading redeemable vouchers", e)
                Toast.makeText(requireContext(), "Lỗi khi tải danh sách voucher: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRedeemConfirmDialog(voucher: Voucher) {
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

        // Đổi tên nút
        btnUseVoucher.text = "Đổi ${voucher.redeemPoints} điểm"

        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnUseVoucher.setOnClickListener {
            redeemVoucher(voucher)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun redeemVoucher(voucher: Voucher) {
        val user = currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để đổi voucher", Toast.LENGTH_SHORT).show()
            return
        }

        if (user.presentPoints < voucher.redeemPoints) {
            Toast.makeText(requireContext(), "Bạn không đủ điểm để đổi voucher này", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val success = userVoucherRepository.redeemVoucherWithPoints(voucher)
                if (success) {
                    // Cập nhật lại điểm hiện tại
                    val updatedUser = userRepository.getCurrentUser()
                    currentUser = updatedUser
                    binding.tvUserPoints.text = "Điểm hiện tại: ${updatedUser?.presentPoints ?: 0}"

                    Toast.makeText(requireContext(), "Đổi voucher thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Đổi voucher thất bại, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RedeemFragment"
        fun newInstance(): RedeemFragment {
            return RedeemFragment()
        }
    }

    inner class RedeemAdapter(
        private var vouchers: List<Voucher>,
        private val onRedeemClick: (Voucher) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<RedeemAdapter.VoucherViewHolder>() {

        fun updateVouchers(newVouchers: List<Voucher>) {
            this.vouchers = newVouchers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_redeem, parent, false)
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
            private val tvRedeemPoints: TextView = itemView.findViewById(R.id.tvRedeemPoints)
            private val btnRedeem: Button = itemView.findViewById(R.id.btnRedeem)

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
                        Log.e(TAG, "Error formatting date", e)
                    }
                } ?: run {
                    tvExpireDate.text = "Không giới hạn"
                }

                // Hiển thị số điểm cần đổi
                tvRedeemPoints.text = "${voucher.redeemPoints} điểm"

                // Thiết lập nút đổi
                btnRedeem.setOnClickListener {
                    onRedeemClick(voucher)
                }

                // Set click listener cho cả item
                itemView.setOnClickListener {
                    onRedeemClick(voucher)
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