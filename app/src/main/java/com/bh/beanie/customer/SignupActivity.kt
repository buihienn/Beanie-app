package com.bh.beanie.customer

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.R
import com.bh.beanie.model.User // Import User model
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.Int

class SignupActivity : AppCompatActivity() {

    // Views
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var avatarPreview: ImageView
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Ánh xạ Views
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        dobEditText = findViewById(R.id.dobEditText)
        genderSpinner = findViewById(R.id.genderSpinner)
        avatarPreview = findViewById(R.id.avatarPreview)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        signUpButton = findViewById(R.id.signUpButton)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.signupProgressBar)

        // --- Setup Gender Spinner ---
        val genderOptions = listOf("Select Gender", "Male", "Female", "Other")
        genderSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)

        // --- Setup Date Picker ---
        dobEditText.setOnClickListener { showDatePickerDialog() }

        // --- Setup Avatar Click (Placeholder) ---
        avatarPreview.setOnClickListener {
            Toast.makeText(this, "Avatar selection feature coming soon!", Toast.LENGTH_SHORT).show()
            // TODO: Implement image picker and upload to Firebase Storage, then update avatarUrl in Firestore
        }

        // --- Setup Sign Up Button Click ---
        signUpButton.setOnClickListener { attemptSignUp() }

        // --- Setup Login Button Click ---
        loginButton.setOnClickListener { goToLogin() }
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, y, m, d ->
            dobEditText.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
        }, year, month, day)

        dpd.datePicker.maxDate = System.currentTimeMillis() // Không cho chọn ngày tương lai
        dpd.show()
    }

    private fun attemptSignUp() {
        // Reset errors
        usernameEditText.error = null
        emailEditText.error = null
        phoneEditText.error = null
        dobEditText.error = null
        passwordEditText.error = null
        confirmPasswordEditText.error = null

        // Lấy giá trị
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val dob = dobEditText.text.toString().trim()
        val selectedGenderPosition = genderSpinner.selectedItemPosition
        val selectedGender = if (selectedGenderPosition > 0) genderSpinner.selectedItem.toString() else ""
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        var cancel = false
        var focusView: View? = null

        // --- Validation ---

        // Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.error = getString(R.string.error_field_required)
            focusView = confirmPasswordEditText
            cancel = true
        } else if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            focusView = confirmPasswordEditText
            cancel = true
        }

        // Password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = getString(R.string.error_field_required)
            focusView = passwordEditText
            cancel = true
        } else if (!isPasswordValid(password)) {
            passwordEditText.error = "Password must be at least 6 characters"
            focusView = passwordEditText
            cancel = true
        }

        // Gender
        if (selectedGenderPosition == 0) { // Vị trí 0 là "Select Gender"
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
            focusView = genderSpinner // focusView có thể là Spinner
            cancel = true
        }

        // Date of Birth
        if (TextUtils.isEmpty(dob)) {
            dobEditText.error = getString(R.string.error_field_required)
            focusView = dobEditText
            cancel = true
        }

        // Phone
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.error = getString(R.string.error_field_required)
            focusView = phoneEditText
            cancel = true
        }
        // TODO: Thêm validation định dạng số điện thoại nếu cần

        // Email
        if (TextUtils.isEmpty(email)) {
            emailEditText.error = getString(R.string.error_field_required)
            focusView = emailEditText
            cancel = true
        } else if (!isEmailValid(email)) {
            emailEditText.error = getString(R.string.error_invalid_email)
            focusView = emailEditText
            cancel = true
        }

        // Username
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = getString(R.string.error_field_required)
            focusView = usernameEditText
            cancel = true
        }
        // --- End Validation ---

        if (cancel) {
            focusView?.requestFocus()
        } else {
            // Validation OK -> Gọi Firebase Auth để tạo user
            showLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Đăng ký Auth thành công -> Lưu thông tin bổ sung vào Firestore
                        Log.d("SIGNUP_SUCCESS", "createUserWithEmail:success")
                        val firebaseUser = auth.currentUser
                        val userId = firebaseUser?.uid

                        if (userId != null) {
                            saveAdditionalUserInfo(userId, username, email, phone, dob, selectedGender)
                        } else {
                            // Lỗi hiếm gặp: không lấy được UID sau khi đăng ký thành công
                            showLoading(false)
                            Log.e("SIGNUP_ERROR", "Failed to get user ID after successful auth registration.")
                            Toast.makeText(baseContext, "Registration failed: Could not save user profile.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Đăng ký Auth thất bại
                        showLoading(false)
                        Log.w("SIGNUP_FAILURE", "createUserWithEmail:failure", task.exception)
                        try {
                            // Xử lý lỗi cụ thể
                            throw task.exception!!
                        } catch (e: FirebaseAuthUserCollisionException) {
                            // Email đã tồn tại
                            emailEditText.error = "The email address is already in use by another account."
                            emailEditText.requestFocus()
                        } catch (e: Exception) {
                            // Lỗi khác
                            Toast.makeText(baseContext, "Registration failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    // Hàm lưu thông tin người dùng (dùng User model) vào Firestore
    private fun saveAdditionalUserInfo(userId: String, username: String, email: String, phone: String, dob: String, gender: String) {
        // Tạo đối tượng User
        val userProfile = User(
            username = username,
            email = email, // Lưu email vào profile luôn
            phone = phone,
            dob = dob,
            gender = gender,
            // role và createdAt sẽ tự động được gán giá trị mặc định/server
        )

        // Lưu vào Firestore collection "users" với document ID là userId
        db.collection("users").document(userId)
            .set(userProfile) // Dùng đối tượng User trực tiếp
            .addOnSuccessListener {
                Log.d("FIRESTORE_SUCCESS", "User profile object successfully written!")
                Toast.makeText(this, "Account created successfully! Please log in.", Toast.LENGTH_LONG).show()
                showLoading(false) // Ẩn loading sau khi lưu thành công
                goToLogin()
                finish() // Đóng SignupActivity
            }
            .addOnFailureListener { e ->
                showLoading(false) // Ẩn loading nếu lỗi
                Log.w("FIRESTORE_FAILURE", "Error writing user profile object", e)
                Toast.makeText(this, "Registration succeeded but failed to save profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                // Cân nhắc: có thể xóa user trong Auth nếu không lưu được profile, hoặc cho phép user đăng nhập và thử cập nhật sau.
                // auth.currentUser?.delete() // Ví dụ xóa user nếu lưu profile lỗi
                goToLogin() // Vẫn cho về login để thử lại
                finish()
            }
    }

    // Hiển thị/ẩn ProgressBar và bật/tắt nút
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUpButton.isEnabled = !isLoading
        loginButton.isEnabled = !isLoading
    }

    // Kiểm tra định dạng email
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Kiểm tra độ dài mật khẩu (Firebase yêu cầu >= 6)
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    // Chuyển sang LoginActivity
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}