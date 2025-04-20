package com.bh.beanie.user.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bh.beanie.repository.UserVoucherRepository
import com.bh.beanie.repository.VoucherRepository
import com.bh.beanie.utils.LuckyWheelView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Random

class LuckyWheelFragment : Fragment() {

    private lateinit var luckyWheelView: LuckyWheelView
    private lateinit var spinButton: Button
    private lateinit var spinCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var userVoucherRepository: UserVoucherRepository
    private lateinit var voucherRepository: VoucherRepository

    private val random = Random()
    private var spinCount = 1 // Default spin count
    private var isSpinning = false
    private var activeVouchers = listOf<Voucher>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lucky_wheel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userVoucherRepository = UserVoucherRepository.getInstance()
        voucherRepository = VoucherRepository.getInstance()

        luckyWheelView = view.findViewById(R.id.luckyWheelView)
        spinButton = view.findViewById(R.id.spinButton)
        spinCountText = view.findViewById(R.id.spinCountText)
        progressBar = view.findViewById(R.id.progressBar)

        updateSpinCountDisplay()

        // Back button handler
        view.findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            requireActivity().onBackPressed()
        }

        spinButton.setOnClickListener {
            if (!isSpinning && spinCount > 0) {
                spinWheel()
            } else if (spinCount <= 0) {
                Toast.makeText(requireContext(), "No spins left", Toast.LENGTH_SHORT).show()
            }
        }

        // Load active vouchers
        loadActiveVouchers()
    }

    private fun loadActiveVouchers() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Use the dedicated VoucherRepository to fetch vouchers
                activeVouchers = voucherRepository.getActiveVouchers()

                Log.d("LuckyWheel", "Loaded ${activeVouchers.size} active vouchers")

                if (activeVouchers.isEmpty()) {
                    // Fallback to get all vouchers if no active ones found
                    activeVouchers = voucherRepository.getAllVouchers()
                    Log.d("LuckyWheel", "Fallback: Loaded ${activeVouchers.size} total vouchers")
                }

                if (activeVouchers.isEmpty()) {
                    Toast.makeText(requireContext(), "No vouchers available at the moment", Toast.LENGTH_SHORT).show()
                    spinButton.isEnabled = false
                } else {
                    // Prepare text values for the wheel
                    val voucherTexts = activeVouchers.map { getVoucherDisplayText(it) }
                    luckyWheelView.setVoucherTexts(voucherTexts)
                }
            } catch (e: Exception) {
                Log.e("LuckyWheel", "Error loading vouchers", e)
                Toast.makeText(requireContext(), "Error loading vouchers: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun getVoucherDisplayText(voucher: Voucher): String {
        return when (voucher.discountType) {
            "PERCENTAGE" -> "${voucher.discountValue.toInt()}%"
            "FIXED" -> "$${voucher.discountValue.toInt()}"
            else -> "${voucher.discountValue.toInt()}"
        }
    }

    private fun updateSpinCountDisplay() {
        spinCountText.text = "Spins left: $spinCount"
    }

    private fun spinWheel() {
        if (activeVouchers.isEmpty()) {
            Toast.makeText(requireContext(), "No vouchers available", Toast.LENGTH_SHORT).show()
            return
        }

        isSpinning = true
        spinButton.isEnabled = false

        // For an 8-segment wheel, each segment is 45 degrees
        val segments = 8
        val degreePerSegment = 360f / segments

        // Randomly decide which segment to land on
        val selectedSegment = random.nextInt(segments)

        // Calculate final rotation: multiple full rotations + position to stop
        val finalRotation = 1800f + (selectedSegment * degreePerSegment)

        luckyWheelView.spin(finalRotation, 4000) {
            isSpinning = false
            spinCount--
            updateSpinCountDisplay()

            // Select a voucher based on the segment
            val selectedVoucher = if (activeVouchers.isNotEmpty()) {
                // Map the selected segment to a voucher index
                val voucherIndex = selectedSegment % activeVouchers.size
                activeVouchers[voucherIndex]
            } else {
                Toast.makeText(requireContext(), "No vouchers available", Toast.LENGTH_SHORT).show()
                spinButton.isEnabled = spinCount > 0
                return@spin
            }

            // Award the voucher to the user
            awardVoucher(selectedVoucher)
            spinButton.isEnabled = spinCount > 0
        }
    }

    private fun awardVoucher(voucher: Voucher) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val success = userVoucherRepository.assignVoucherToUser(voucher)

                if (success) {
                    showVoucherWonDialog(voucher)
                } else {
                    Toast.makeText(requireContext(), "Failed to assign voucher", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LuckyWheel", "Error awarding voucher", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showVoucherWonDialog(voucher: Voucher) {
        val discountText = when (voucher.discountType) {
            "PERCENTAGE" -> "${voucher.discountValue}% off"
            "FIXED" -> "$${voucher.discountValue} off"
            else -> "${voucher.discountValue} off"
        }

        val minOrderText = voucher.minOrderAmount?.let {
            "Minimum order: $${it}"
        } ?: "No minimum order"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Congratulations!")
            .setMessage("You won: ${voucher.name}\n$discountText\n$minOrderText\n\n${voucher.content}")
            .setPositiveButton("Claim") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        spinButton.isEnabled = !isLoading && spinCount > 0
    }

    companion object {
        fun newInstance() = LuckyWheelFragment()
    }
}