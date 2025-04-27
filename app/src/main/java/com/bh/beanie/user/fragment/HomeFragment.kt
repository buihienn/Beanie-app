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
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentHomeBinding
import com.bh.beanie.model.Product
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.BeanieApplication
import com.bh.beanie.repository.NotificationRepository
import com.bh.beanie.repository.UserRepository
import com.bh.beanie.user.NotificationsActivity
import com.bh.beanie.user.UserOrderActivity
import com.bh.beanie.user.adapter.BestSellerAdapter
import com.bh.beanie.utils.BranchPreferences.getBranchId
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import kotlin.text.replace

@OptIn(ExperimentalBadgeUtils::class)
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bestSellerAdapter: BestSellerAdapter
    private val bestSellerProducts = mutableListOf<Product>()

    private lateinit var productRepository: ProductRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationRepository: NotificationRepository

    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo repository
        productRepository = ProductRepository(FirebaseFirestore.getInstance())
        userRepository = UserRepository()
        notificationRepository = NotificationRepository()

        userId = BeanieApplication.instance.getUserId()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Sử dụng view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        if (userId.isNotEmpty()) {
            fetchUserPoints()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFortuneWheelCard()
        setupRecyclerView()
        setupButtons()
        loadBestSellerProducts()

        // Lấy userId và tạo barcode nếu đã đăng nhập
        if (userId.isNotEmpty()) {
            generateAndSetBarcode(userId)
        }
    }

    private fun setupRecyclerView() {
        bestSellerAdapter = BestSellerAdapter(bestSellerProducts) { product ->
            // Xử lý khi click vào sản phẩm
            val orderFragment = OrderFragment.newInstance()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, orderFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.popularItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = bestSellerAdapter
        }
    }

    private fun setupButtons() {
        binding.notificationButton.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        binding.deliveryButton.setOnClickListener {
            showAddressSelectionAndOpenCategories()
        }

        binding.takeawayButton.setOnClickListener {
            showBranchSelectionAndOpenCategories()
        }

        binding.redeemButton.setOnClickListener {
            val redeemFragment = RedeemFragment.newInstance()
            redeemFragment.show(parentFragmentManager, RedeemFragment.TAG)
        }

        binding.membershipInfoButton.setOnClickListener {
            val rewardFragment = RewardFragment.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, rewardFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadBestSellerProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val branchId = getBranchId(requireContext())
                val products = productRepository.fetchBestSellersSuspend(branchId)

                bestSellerProducts.clear()
                bestSellerProducts.addAll(products)
                bestSellerAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading best sellers: ${e.message}")
            }
        }
    }

    private fun generateAndSetBarcode(userId: String) {
        try {
            // Tạo barcode từ userId
            val barcodeBitmap = generateBarcode(userId, BarcodeFormat.CODE_128, 350, 150)

            // Gán bitmap vào ImageView
            binding.ivBarcode.setImageBitmap(barcodeBitmap)
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

    private fun setupFortuneWheelCard() {
        // Set onClick listener to open LuckyWheel fragment
        binding.fortuneWheelCard.setOnClickListener {
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
            putString("order_mode", orderMode)
        }
    }

    @ExperimentalBadgeUtils
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

    @ExperimentalBadgeUtils
    private fun updateNotificationBadge(count: Int) {
        if (count > 0 && _binding != null) {
            // Create a badge drawable
            val badgeDrawable = BadgeDrawable.create(requireContext())
            badgeDrawable.number = count
            badgeDrawable.backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorError)

            // Position the badge on the notification button
            BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.notificationButton, null)
        }
    }

    private fun fetchUserPoints() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                user?.let {
                    // Cập nhật UI với số điểm hiện tại của user
                    binding.beaniesCountText.text = it.presentPoints.toString()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching user points: ${e.message}")
                // Hiển thị giá trị mặc định nếu có lỗi
                binding.beaniesCountText.text = "0"
            }
        }
    }

    @ExperimentalBadgeUtils
    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) {
            checkUnreadNotifications()
            fetchUserPoints()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}