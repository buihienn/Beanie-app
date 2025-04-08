package com.bh.beanie.user.fragment

import android.content.Intent // <-- Thêm import này
import android.os.Bundle
import android.util.Log // <-- Thêm import để log lỗi (tùy chọn)
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // <-- Hoặc dùng MaterialButton nếu cần
import com.google.android.material.button.MaterialButton // <-- Import đúng loại Button
import com.bh.beanie.R
import com.bh.beanie.customer.LoginActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.user.adapter.ProductAdapter
import com.bh.beanie.user.model.Product


// Xóa các tham số và TODO không dùng đến nếu bạn không cần chúng
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {

    // Xóa các tham số không dùng đến
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var popularItemsRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Xóa phần xử lý arguments nếu không dùng
         arguments?.let {
             param1 = it.getString(ARG_PARAM1)
             param2 = it.getString(ARG_PARAM2)
         }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // Giữ nguyên cách inflate layout thông thường
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // --- BẮT ĐẦU: Thêm code xử lý click với findViewById ---

        // Tìm Button bằng ID của nó trong view đã được inflate
        // Sử dụng kiểu cụ thể là MaterialButton vì nó được định nghĩa trong XML
        val notificationButton = view.findViewById<MaterialButton>(R.id.notificationButton)

        // Kiểm tra xem button có được tìm thấy không (đề phòng lỗi)
        if (notificationButton != null) {
            notificationButton.setOnClickListener {
                // Tạo Intent để mở LoginActivity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                // Khởi chạy Activity
                startActivity(intent)
                // Optional: Nếu bạn muốn đóng Fragment/Activity hiện tại sau khi mở LoginActivity
                // requireActivity().finish() // Bỏ comment nếu cần

        // Khởi tạo RecyclerView
        popularItemsRecyclerView = view.findViewById(R.id.popularItemsRecyclerView)
        popularItemsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Tạo dữ liệu mẫu
        createSampleData()

        // Khởi tạo adapter và gắn vào RecyclerView
        productAdapter = ProductAdapter(requireContext(), productList)
        popularItemsRecyclerView.adapter = productAdapter
    }

    private fun createSampleData() {
        // Thêm sản phẩm mẫu từ hình ảnh
        productList.apply {
            add(Product(1, "Coffee extra milk", 35000.0, R.drawable.matcha))
            add(Product(2, "Olong Blao Milktea", 35000.0, R.drawable.matcha))
            add(Product(3, "Matcha latte", 35000.0, R.drawable.matcha))
            add(Product(4, "Café latte", 35000.0, R.drawable.matcha))
            add(Product(5, "Café latte", 35000.0, R.drawable.matcha))
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }

            }
        } else {
            // Log lỗi nếu không tìm thấy button (hữu ích khi debug)
            Log.e("HomeFragment", "Could not find notificationButton with ID R.id.notificationButton")
        }
        // --- KẾT THÚC: Thêm code xử lý click với findViewById ---

        // Các thiết lập UI khác có thể đặt ở đây
        // ví dụ: val welcomeTextView = view.findViewById<TextView>(R.id.welcomeTextView)
        // welcomeTextView?.text = "Chào mừng User!"
    }

    // Lưu ý: Không cần onDestroyView để clear binding khi dùng findViewById

    // Xóa companion object nếu không cần tạo instance với tham số
     companion object {
         @JvmStatic
         fun newInstance(/* Bỏ params nếu không dùng */) =
             HomeFragment().apply {
                 arguments = Bundle().apply {
                     // putString(ARG_PARAM1, param1)
                     // putString(ARG_PARAM2, param2)
                 }
             }
     }
}