package com.bh.beanie.user.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Product
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.BeanieApplication
import com.bh.beanie.repository.NotificationRepository
import com.bh.beanie.user.NotificationsActivity
import com.bh.beanie.user.UserOrderActivity
import com.bh.beanie.user.adapter.ProductAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
@OptIn(com.google.android.material.badge.ExperimentalBadgeUtils::class)

class HomeFragment : Fragment() {
    @OptIn(com.google.android.material.badge.ExperimentalBadgeUtils::class)
    private lateinit var popularItemsRecyclerView: RecyclerView
    private val productList = mutableListOf<Product>()
    private lateinit var productRepository: ProductRepository
    private lateinit var barcodeImageView: ImageView
    private lateinit var notificationRepository: NotificationRepository
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo repository
        productRepository = ProductRepository(FirebaseFirestore.getInstance())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFortuneWheelCard(view)
        userId = BeanieApplication.instance.getUserId()
        barcodeImageView = view.findViewById(R.id.ivBarcode)

        // Khởi tạo RecyclerView
        popularItemsRecyclerView = view.findViewById(R.id.popularItemsRecyclerView)
        popularItemsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        notificationRepository = NotificationRepository()
        // Lấy userId
        val userId = BeanieApplication.instance.getUserId()

        if (userId.isNotEmpty()) {
            // Tạo barcode từ userId
            generateAndSetBarcode(userId)
        }

        val notificationButton = view.findViewById<MaterialButton>(R.id.notificationButton)
        notificationButton.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        val deliveryBtn: Button = view.findViewById(R.id.deliveryButton)
        deliveryBtn.setOnClickListener {
            showAddressSelectionAndOpenCategories()
        }

        val takeAwayBtn: Button = view.findViewById(R.id.takeawayButton)
        takeAwayBtn.setOnClickListener {
            showBranchSelectionAndOpenCategories()
        }
    }

    private fun generateAndSetBarcode(userId: String) {
        try {
            // Tạo barcode từ userId
            val barcodeBitmap = generateBarcode(userId, BarcodeFormat.CODE_128, 350, 150)

            // Gán bitmap vào ImageView
            barcodeImageView.setImageBitmap(barcodeBitmap)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Lỗi khi tạo mã barcode: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun generateBarcode(content: String, format: BarcodeFormat, width: Int, height: Int): Bitmap {
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, format, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    private fun setupFortuneWheelCard(view: View) {
        // Get reference to the Fortune Wheel card
        val fortuneWheelCard = view.findViewById<MaterialCardView>(R.id.fortuneWheelCard)

        // Set onClick listener to open LuckyWheel fragment
        fortuneWheelCard.setOnClickListener {
            // Create instance of LuckyWheelFragment
            val luckyWheelFragment = LuckyWheelFragment.newInstance()

            // Replace current fragment with LuckyWheelFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, luckyWheelFragment)
                .addToBackStack(null)
                .commit()
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
    
    private fun checkUnreadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val unreadCount = notificationRepository.getUnreadCount(userId)
                updateNotificationBadge(unreadCount)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error checking notifications", e)
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            val notificationButton = view?.findViewById<MaterialButton>(R.id.notificationButton)
            if (notificationButton != null) {
                // Create a badge drawable
                val badgeDrawable = BadgeDrawable.create(requireContext())
                badgeDrawable.number = count
                badgeDrawable.backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorError)

                // Position the badge on the notification button
                BadgeUtils.attachBadgeDrawable(badgeDrawable, notificationButton, null)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) {
            checkUnreadNotifications()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}