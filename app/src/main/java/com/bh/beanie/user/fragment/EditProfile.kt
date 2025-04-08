package com.bh.beanie.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.* // Import các lớp View cần thiết
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bh.beanie.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText

/**
 * A simple [Fragment] subclass.
 * Use the [EditProfile.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditProfile : Fragment() {
    // TODO: Rename and change types of parameters
    // --- Khai báo biến cho các View ---
    // Khai báo là lateinit vì chúng sẽ được khởi tạo trong onViewCreated
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvProfileTitle: TextView
    private lateinit var ivProfilePic: ShapeableImageView
    private lateinit var tvUserName: TextView
    private lateinit var etEmailEditable: TextInputEditText // Hoặc EditText nếu không dùng TextInputLayout
    private lateinit var etEmailDisabled1: TextInputEditText
    private lateinit var etEmailDisabled2: TextInputEditText
    private lateinit var actvEmailDropdown: AutoCompleteTextView
    private lateinit var btnUpdateProfile: MaterialButton // Hoặc Button
    private lateinit var btnDeleteAccount: MaterialButton // Hoặc Button

    // --- onCreateView ---
    // Chỉ inflate layout và trả về View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout XML bằng cách truyền thống
        // Trả về view gốc của layout đã inflate
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    // --- onViewCreated ---
    // Tìm và thiết lập các View sau khi layout đã được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Ánh xạ Views sử dụng findViewById ---
        // Sử dụng 'view' được truyền vào hàm này để gọi findViewById
        ivBackArrow = view.findViewById(R.id.ivBackArrow)
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle) // Mặc dù không dùng trong logic nhưng vẫn có thể ánh xạ
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        tvUserName = view.findViewById(R.id.tvUserName)
        etEmailEditable = view.findViewById(R.id.etEmailEditable) // ID của TextInputEditText bên trong TextInputLayout
        etEmailDisabled1 = view.findViewById(R.id.etEmailDisabled1)
        etEmailDisabled2 = view.findViewById(R.id.etEmailDisabled2)
        actvEmailDropdown = view.findViewById(R.id.actvEmailDropdown)
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)


        // --- Thiết lập Dữ liệu cho Dropdown (AutoCompleteTextView) ---
        setupDropdown()

        // --- Thiết lập Listener cho các nút ---
        setupButtonClickListeners()
    }

    // --- Thiết lập Dropdown ---
    private fun setupDropdown() {
        val items = listOf("Email Option 1", "Email Option 2", "Email Option 3")
        // Cần Context để tạo Adapter, dùng requireContext() là an toàn trong Fragment sau onViewCreated
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        // Gán Adapter cho AutoCompleteTextView đã ánh xạ
        actvEmailDropdown.setAdapter(adapter)
    }

    // --- Thiết lập Listener ---
    private fun setupButtonClickListeners() {
        // Nút Back Arrow
        ivBackArrow.setOnClickListener {
            //findNavController().navigateUp() // Vẫn dùng Navigation Component hoặc cách khác
        }

        // Nút Update Profile
        btnUpdateProfile.setOnClickListener {
            val emailEditable = etEmailEditable.text.toString()
            val emailDisabled1 = etEmailDisabled1.text.toString()
            val emailDisabled2 = etEmailDisabled2.text.toString()
            val emailDropdown = actvEmailDropdown.text.toString()

            // TODO: Validate và xử lý logic cập nhật
            Toast.makeText(requireContext(), "Update Profile Clicked (findViewById)", Toast.LENGTH_SHORT).show()
            println("Email Editable: $emailEditable")
            println("Email Dropdown: $emailDropdown")
        }

        // Nút Delete Account
        btnDeleteAccount.setOnClickListener {
            // TODO: Xử lý logic xóa tài khoản
            Toast.makeText(requireContext(), "Delete Account Clicked (findViewById)", Toast.LENGTH_SHORT).show()
        }
    }

    // --- onDestroyView ---
    // Không cần làm gì đặc biệt ở đây khi không dùng View Binding
    // override fun onDestroyView() {
    //     super.onDestroyView()
    // }
}