package com.bh.beanie.customer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.MainActivity // Đảm bảo import đúng MainActivity của bạn
import com.bh.beanie.R
// import com.bh.beanie.SignupActivity // Đảm bảo import đúng SignupActivity của bạn
// Giả sử bạn có SignupActivity, nếu không thì comment dòng trên và phần goToSignUp

class LoginActivity : AppCompatActivity() {

    // Khai báo biến cho các View
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var txtForgotPassword: TextView
    private lateinit var btnLoginGoogle: Button
    private lateinit var txtSignUpLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Đảm bảo tên layout đúng

        // Ánh xạ views
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        txtForgotPassword = findViewById(R.id.txtForgotPassword)
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle)
        txtSignUpLink = findViewById(R.id.txtSignUpLink)

        // --- Xử lý sự kiện click ---

        // Nhấn nút "Sign in" -> Chuyển đến MainActivity
        btnSignIn.setOnClickListener {
            // Lấy thông tin nhập liệu (tùy chọn, có thể bỏ qua nếu không cần)
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            println("Email: $email, Password: $password") // In ra logcat để xem

            // Chỉ chuyển màn hình, không kiểm tra gì cả
            goToMainActivity()
        }

        // Nhấn liên kết "Forgot password?" -> Hiển thị Toast
        txtForgotPassword.setOnClickListener {
            showForgotPasswordMessage()
        }

        // Nhấn nút "Sign in with Google" -> Hiển thị Toast
        btnLoginGoogle.setOnClickListener {
            showGoogleSignInMessage()
        }

        // Nhấn liên kết "Sign up" -> Chuyển đến SignupActivity
        txtSignUpLink.setOnClickListener {
            goToSignUp()
        }
    }

    // --- Các hàm xử lý đơn giản ---

    // Hiển thị thông báo tạm thời cho chức năng Quên mật khẩu
    private fun showForgotPasswordMessage() {
        Toast.makeText(this, "Chức năng Quên mật khẩu chưa được cài đặt.", Toast.LENGTH_SHORT).show()
        // Hoặc:
        // val intent = Intent(this, ForgotPasswordActivity::class.java) // Nếu có màn hình riêng
        // startActivity(intent)
    }

    // Hiển thị thông báo tạm thời cho chức năng Đăng nhập Google
    private fun showGoogleSignInMessage() {
        Toast.makeText(this, "Chức năng Đăng nhập Google chưa được cài đặt.", Toast.LENGTH_SHORT).show()
    }

    // Chuyển đến màn hình đăng ký
    private fun goToSignUp() {
        // Đảm bảo bạn có SignupActivity và đã khai báo trong Manifest
        val intent = Intent(this, SignupActivity::class.java) // Bỏ comment nếu có SignupActivity
        startActivity(intent) // Bỏ comment nếu có SignupActivity
        // Toast.makeText(this, "Chuyển đến màn hình Đăng ký (chưa có).", Toast.LENGTH_SHORT).show() // Dùng tạm nếu chưa có SignupActivity
    }

    // Chuyển đến màn hình chính
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Xóa các activity trước đó khỏi stack để người dùng không quay lại màn hình login bằng nút back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Đóng LoginActivity hiện tại
    }
}