package com.bh.beanie.user.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Product
import com.bh.beanie.repository.ProductRepository
import com.bh.beanie.BeanieApplication
import com.bh.beanie.user.UserOrderActivity
import com.bh.beanie.user.adapter.ProductAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var popularItemsRecyclerView: RecyclerView
    private val productList = mutableListOf<Product>()
    private lateinit var productRepository: ProductRepository
    private lateinit var barcodeImageView: ImageView

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

        val notificationButton = view.findViewById<MaterialButton>(R.id.notificationButton)
        barcodeImageView = view.findViewById(R.id.ivBarcode)

        // Khởi tạo RecyclerView
        popularItemsRecyclerView = view.findViewById(R.id.popularItemsRecyclerView)
        popularItemsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Lấy userId
        val userId = BeanieApplication.instance.getUserId()

        if (userId.isNotEmpty()) {
            // Tạo barcode từ userId
            generateAndSetBarcode(userId)
        }

        val deliveryBtn: Button = view.findViewById(R.id.deliveryButton)
        deliveryBtn.setOnClickListener {
            startActivity(Intent(requireContext(), UserOrderActivity::class.java))
        }

        val takeAwayBtn: Button = view.findViewById(R.id.takeawayButton)
        takeAwayBtn.setOnClickListener {
            startActivity(Intent(requireContext(), UserOrderActivity::class.java))
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

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}