package com.bh.beanie.customer

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.MainActivity
import com.bh.beanie.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignupActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var tvBirthDate: TextView // Thay đổi
    private lateinit var btnPickDate: ImageButton // Thay đổi
    private lateinit var rgGender: RadioGroup
    private lateinit var btnSignUp: Button
    private lateinit var btnSignUpGoogle: Button
    private lateinit var tvLogin: TextView
    // private lateinit var mGoogleSignInClient: GoogleSignInClient  // Bỏ comment khi cần
    private val RC_SIGN_IN: Int = 456
    private val calendar = Calendar.getInstance() // Thêm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Ánh xạ views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        tvBirthDate = findViewById(R.id.tvBirthDate) // Thay đổi
        btnPickDate = findViewById(R.id.btnPickDate) // Thay đổi
        rgGender = findViewById(R.id.rgGender)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvLogin = findViewById(R.id.tvLogin)
        btnSignUpGoogle = findViewById(R.id.btnSignUpGoogle)


        // Xử lý sự kiện click cho nút Sign Up
        btnSignUp.setOnClickListener {
            signUp()
        }

        // Xử lý Google Sign In, gọi hàm nhưng hàm đang để trống
        btnSignUpGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Xử lý sự kiện click cho liên kết "Login"
        tvLogin.setOnClickListener {
            goToLogin()
        }

        // Xử lý sự kiện click cho nút chọn ngày
        btnPickDate.setOnClickListener {
            showDatePickerDialog()
        }

    }
    //Hàm google sign in
    private fun signInWithGoogle() {
        // TODO: Implement Google Sign-In here later
    }

    private fun signUp() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        //val birthYear = etBirthYear.text.toString().trim() // Bỏ
        val birthDate = tvBirthDate.text.toString().trim() // Lấy từ TextView
        val gender = getSelectedGender()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() ||
            confirmPassword.isEmpty() || phoneNumber.isEmpty() || birthDate.isEmpty() || gender.isEmpty()
        ) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Kiểm tra định dạng email, số điện thoại, năm sinh...
        // TODO: Thực hiện đăng ký (gọi API, lưu vào database, ...)
        // Tạm thời, hiển thị Toast để mô phỏng

        Toast.makeText(this, "Đăng ký (chưa implement backend)", Toast.LENGTH_SHORT).show()

        // Ví dụ: Chuyển đến MainActivity và truyền dữ liệu (bạn có thể thay đổi cách truyền dữ liệu)
        val intent = Intent(this, MainActivity::class.java) // Thay MainActivity
        intent.putExtra("FULL_NAME", fullName)
        intent.putExtra("EMAIL", email)
        intent.putExtra("PHONE_NUMBER", phoneNumber)
        intent.putExtra("BIRTH_DATE", birthDate) // Truyền chuỗi ngày tháng
        intent.putExtra("GENDER", gender)
        // ... truyền các thông tin khác nếu cần
        startActivity(intent)
        finish() // Close Signup

    }
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)  //Thay main activity
        startActivity(intent)
        finish() // Đóng SignupActivity
    }
    private fun getSelectedGender(): String {
        val selectedId = rgGender.checkedRadioButtonId
        return if (selectedId != -1) {
            findViewById<RadioButton>(selectedId).text.toString()
        } else {
            "" // Trả về chuỗi rỗng nếu không có lựa chọn nào
        }
    }
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    // Thêm hàm hiển thị DatePickerDialog
    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Xử lý khi người dùng chọn ngày
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Thêm hàm cập nhật TextView với ngày tháng năm đã chọn
    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy" // Định dạng bạn muốn
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        tvBirthDate.text = sdf.format(calendar.time)
    }
}