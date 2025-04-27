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
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.repository.UserVoucherRepository
import com.bh.beanie.repository.VoucherRepository
import com.bh.beanie.repository.NotificationRepository
import com.bh.beanie.utils.LuckyWheelView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Random
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest

class LuckyWheelFragment : Fragment() {

    private lateinit var luckyWheelView: LuckyWheelView
    private lateinit var spinButton: Button
    private lateinit var spinCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var userVoucherRepository: UserVoucherRepository
    private lateinit var voucherRepository: VoucherRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationRepository: NotificationRepository


    private val random = Random()
    private var isSpinning = false
    private var activeVouchers = listOf<Voucher>()
    private var userId: String = ""

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
        userRepository = UserRepository.getInstance()
        notificationRepository = NotificationRepository()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        luckyWheelView = view.findViewById(R.id.luckyWheelView)
        spinButton = view.findViewById(R.id.spinButton)
        spinCountText = view.findViewById(R.id.spinCountText)
        progressBar = view.findViewById(R.id.progressBar)

        checkNotificationPermission()
        // Back button handler
        view.findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            requireActivity().onBackPressed()
        }

        spinButton.setOnClickListener {
            if (!isSpinning) {
                checkAndSpin()
            }
        }

        // Load active vouchers and check spin availability
        loadActiveVouchersAndCheckSpin()
    }

    private fun loadActiveVouchersAndCheckSpin() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Check if user can spin today
                val canSpin = userRepository.canUserSpinToday(userId)

                // Load vouchers
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

                // Update UI based on spin availability
                updateSpinAvailability(canSpin)
            } catch (e: Exception) {
                Log.e("LuckyWheel", "Error loading data", e)
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateSpinAvailability(canSpin: Boolean) {
        spinButton.isEnabled = canSpin && activeVouchers.isNotEmpty()
        spinCountText.text = if (canSpin) "Spins left: 1" else "No spins left today"

        if (!canSpin) {
            spinButton.text = "Come back tomorrow"
        } else {
            spinButton.text = "SPIN"
        }
    }

    private fun checkAndSpin() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val canSpin = userRepository.canUserSpinToday(userId)

                if (canSpin) {
                    spinWheel()
                } else {
                    Toast.makeText(requireContext(), "You've already spun today! Come back tomorrow.", Toast.LENGTH_SHORT).show()
                    updateSpinAvailability(false)
                }
            } catch (e: Exception) {
                Log.e("LuckyWheel", "Error checking spin eligibility", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

            // Update the last spin date in Firestore
            lifecycleScope.launch {
                try {
                    userRepository.updateUserLastSpinDate(userId)
                    updateSpinAvailability(false)
                } catch (e: Exception) {
                    Log.e("LuckyWheel", "Error updating spin date", e)
                }
            }

            // Select a voucher based on the segment
            val selectedVoucher = if (activeVouchers.isNotEmpty()) {
                // Map the selected segment to a voucher index
                val voucherIndex = selectedSegment % activeVouchers.size
                activeVouchers[voucherIndex]
            } else {
                Toast.makeText(requireContext(), "No vouchers available", Toast.LENGTH_SHORT).show()
                return@spin
            }

            // Award the voucher to the user
            awardVoucher(selectedVoucher)
        }
    }

    private fun awardVoucher(voucher: Voucher) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val success = userVoucherRepository.assignVoucherToUser(voucher)

                if (success) {
                    // Show dialog
                    showVoucherWonDialog(voucher)

                    // Create Firestore notification
                    createVoucherNotification(voucher)

                    // Send local phone notification
                    sendVoucherNotification(voucher)
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

    // Add this new function to create Firestore notification
    private fun createVoucherNotification(voucher: Voucher) {
        lifecycleScope.launch {
            try {
                val discountText = when (voucher.discountType) {
                    "PERCENTAGE" -> "${voucher.discountValue}% off"
                    "FIXED" -> "$${voucher.discountValue} off"
                    else -> "${voucher.discountValue} off"
                }

                // Check your Voucher class - if the property isn't called 'code'
                // replace this with the actual property name like 'voucherId' or 'voucherNumber'
                val voucherCode = voucher.id // Using ID as fallback if code doesn't exist

                val notification = com.bh.beanie.model.Notification(
                    id = "", // Will be set by Firestore
                    userId = userId,
                    title = "New Voucher Received!",
                    message = "You won a ${voucher.name}: $discountText",
                    type = "VOUCHER",
                    read = false,
                    timestamp = System.currentTimeMillis(),
                    data = mapOf(
                        "voucherCode" to voucherCode,
                        "voucherValue" to voucher.discountValue.toString()
                    )
                )

                notificationRepository.addNotification(notification)
                Log.d("LuckyWheel", "Firestore notification created for voucher: ${voucher.name}")
            } catch (e: Exception) {
                Log.e("LuckyWheel", "Failed to create Firestore notification", e)
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

    private fun sendVoucherNotification(voucher: Voucher) {
        try {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "voucher_channel"
                val channelName = "Voucher Notifications"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = "Notifications for new vouchers"
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Format discount text for notification
            val discountText = when (voucher.discountType) {
                "PERCENTAGE" -> "${voucher.discountValue}% off"
                "FIXED" -> "$${voucher.discountValue} off"
                else -> "${voucher.discountValue} off"
            }

            // Build the notification
            val notificationId = System.currentTimeMillis().toInt()
            val intent = Intent(context, requireActivity()::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("openVouchers", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(requireContext(), "voucher_channel")
                .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
                .setContentTitle("New Voucher Received!")
                .setContentText("You won a ${voucher.name}: $discountText")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("You won a ${voucher.name}: $discountText\n${voucher.content}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, builder.build())

            Log.d("LuckyWheel", "Notification sent for voucher: ${voucher.name}")
        } catch (e: Exception) {
            Log.e("LuckyWheel", "Error sending notification", e)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LuckyWheel", "Notification permission granted")
        } else {
            Log.d("LuckyWheel", "Notification permission denied")
            Toast.makeText(
                context,
                "Notification permission denied. You won't receive voucher notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        spinButton.isEnabled = !isLoading
    }

    companion object {
        fun newInstance() = LuckyWheelFragment()
    }
}