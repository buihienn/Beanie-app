package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.UserVoucherRepository
import com.bh.beanie.repository.VoucherRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherFragment : Fragment() {

    private lateinit var rvVouchers: RecyclerView
    private lateinit var voucherAdapter: VoucherAdapter
    private lateinit var voucherRepository: VoucherRepository
    private lateinit var userVoucherRepository: UserVoucherRepository

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID", "")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_voucher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repositories
        voucherRepository = VoucherRepository()
        userVoucherRepository = UserVoucherRepository()

        // Initialize RecyclerView
        rvVouchers = view.findViewById(R.id.rvVouchers)
        rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        voucherAdapter = VoucherAdapter(emptyList()) { voucher ->
            selectVoucher(voucher)
        }
        rvVouchers.adapter = voucherAdapter

        // Load vouchers
        loadVouchers()
    }

    private fun loadVouchers() {
        lifecycleScope.launch {
            try {
                Log.d("VoucherFragment", "Fetching user vouchers...")
                Log.d("VoucherFragment", "User ID: $userId")
                val userVouchers = userVoucherRepository.getUserVouchers()

                Log.d("VoucherFragment", "Retrieved ${userVouchers.size} user vouchers")
                userVouchers.forEachIndexed { index, pair ->
                    Log.d("VoucherFragment", "Voucher $index: UserVoucher(id=${pair.first.id}, voucherId=${pair.first.voucherId}), " +
                            "Voucher(id=${pair.second.id}, discountValue=${pair.second.discountValue}, discountType=${pair.second.discountType})")
                }

                if (userVouchers.isNotEmpty()) {
                    // Extract just the Voucher objects from the pairs
                    val vouchers = userVouchers.map { it.second }
                    Log.d("VoucherFragment", "Updating adapter with ${vouchers.size} vouchers")
                    voucherAdapter.updateVouchers(vouchers)
                } else {
                    Log.d("VoucherFragment", "No vouchers found in the response")
                    Toast.makeText(requireContext(), "No vouchers available at the moment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VoucherFragment", "Error loading vouchers", e)
                Log.e("VoucherFragment", "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(requireContext(), "Error loading vouchers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectVoucher(voucher: Voucher) {
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

        tvDialogMinOrder.text = voucher.minOrderAmount?.let {
            "${it.toInt()}K"
        } ?: "No minimum"

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvDialogExpiry.text = voucher.expiryDate?.let {
            dateFormat.format(Date(it.seconds * 1000))
        } ?: "No expiry"

        tvDialogDescription.text = voucher.content ?: "No description available"

        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnUseVoucher.setOnClickListener {
            // Assign voucher to user when the Use Now button is clicked
            assignVoucherToUser(voucher)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun assignVoucherToUser(voucher: Voucher) {
        lifecycleScope.launch {
            try {
                val success = userVoucherRepository.assignVoucherToUser(voucher)
                if (success) {
                    Toast.makeText(requireContext(), "Voucher selected successfully", Toast.LENGTH_SHORT).show()
                    // Refresh voucher list
                    loadVouchers()
                } else {
                    Toast.makeText(requireContext(), "Failed to select voucher", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VoucherFragment", "Error selecting voucher", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        fun newInstance(userId: String) = VoucherFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", userId)
            }
        }
    }

    inner class VoucherAdapter(
        private var vouchers: List<Voucher>,
        private val onSelectClick: (Voucher) -> Unit
    ) : RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>() {

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

        inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
                val minOrderText = voucher.minOrderAmount?.let {
                    "FROM ${it.toInt()}K"
                } ?: ""

                tvVoucherContent.text = "${getDiscountText(voucher)} OFF $minOrderText"

                // Set expiration date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                voucher.expiryDate?.let { expiryDate ->
                    try {
                        // Since expiryDate is already a Timestamp
                        val date = Date(expiryDate.seconds * 1000)
                        tvExpireDate.text = dateFormat.format(date)
                    } catch (e: Exception) {
                        tvExpireDate.text = "Invalid date"
                        Log.e("VoucherFragment", "Error formatting date", e)
                    }
                } ?: run {
                    tvExpireDate.text = "No expiry"
                }

                // Set button click listener
                btnSelect.setOnClickListener {
                    onSelectClick(voucher)
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