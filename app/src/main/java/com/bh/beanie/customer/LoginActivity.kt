package com.bh.beanie.customer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.MainActivity
import com.bh.beanie.R

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnLoginGoogle: Button
    private lateinit var tvRegister: TextView
    //private lateinit var mGoogleSignInClient: GoogleSignInClient // Bỏ comment khi cần dùng
    private val RC_SIGN_IN: Int = 123 // Bạn có thể chọn bất kỳ số nguyên nào


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Ánh xạ views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle)
        tvRegister = findViewById(R.id.tvRegister)

        // Xử lý sự kiện click cho nút Login
        btnLogin.setOnClickListener {
            login()
        }

        // Xử lý sự kiện click cho liên kết "Forgot Password?"
        tvForgotPassword.setOnClickListener {
            forgotPassword()
        }

        // Xử lý sự kiện click cho nút Login with Google (để trống hàm)
        btnLoginGoogle.setOnClickListener {
            signInWithGoogle() // Gọi hàm, nhưng hàm đang để trống
        }

        // Xử lý sự kiện click cho liên kết "Register"
        tvRegister.setOnClickListener {
            goToSignUp()
        }
    }

    // Hàm xử lý đăng nhập bằng Google (để trống)
    private fun signInWithGoogle() {
        // TODO: Implement Google Sign-In here later
    }

    // Hàm xử lý kết quả trả về từ Google Sign-In (để trống)
    // (Bạn sẽ cần hàm này khi implement Google Sign-In)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == RC_SIGN_IN) {
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//            handleSignInResult(task) // Gọi hàm handle, nhưng hàm đang để trống.
//        }
//    }
    // Hàm để handle kết quả đăng nhập, cũng để trống.
    private fun handleSignInResult(){

    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Thực hiện xác thực (gọi API, kiểm tra database, ...)
        // Tạm thời, hiển thị Toast để mô phỏng
        Toast.makeText(this, "Đăng nhập (chưa implement backend)", Toast.LENGTH_SHORT).show()
        goToMainActivity() // Giả sử đăng nhập thành công
    }

    private fun forgotPassword() {
        // TODO: Chuyển đến màn hình Forgot Password (nếu có)
        Toast.makeText(this, "Chuyển đến màn hình Forgot Password (chưa implement)", Toast.LENGTH_SHORT).show()
    }



    private fun goToSignUp() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }
    private fun goToMainActivity(){
        val intent = Intent(this, MainActivity::class.java) // Thay MainActivity bằng activity chính
        startActivity(intent)
        finish() // Đóng LoginActivity
    }
}