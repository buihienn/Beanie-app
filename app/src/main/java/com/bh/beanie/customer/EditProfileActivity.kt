package com.bh.beanie.customer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bh.beanie.R
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etGender: TextInputEditText
    private lateinit var tvPhoneNumber: TextView // Thay đổi
    private lateinit var tvPassword: TextView    // Thay đổi
    private lateinit var btnEditPhone: Button      // Thay đổi
    private lateinit var btnEditPassword: Button   // Thay đổi

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Ánh xạ views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etBirthDate = findViewById(R.id.etBirthDate)
        etGender = findViewById(R.id.etGender)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber) // Thay đổi
        tvPassword = findViewById(R.id.tvPassword)       // Thay đổi
        btnEditPhone = findViewById(R.id.btnEditPhone)     // Thay đổi
        btnEditPassword = findViewById(R.id.btnEditPassword)  // Thay đổi
        toolbar = findViewById(R.id.toolbar)

        //Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Lấy thông tin (placeholder)
        val fullName = intent.getStringExtra("FULL_NAME") ?: "John Doe"
        val email = intent.getStringExtra("EMAIL") ?: "john.doe@example.com"
        val birthDate = intent.getStringExtra("BIRTH_DATE") ?: "01/01/1990"
        val gender = intent.getStringExtra("GENDER") ?: "Male"
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "123-456-7890"


        etFullName.setText(fullName)
        etEmail.setText(email)
        etBirthDate.setText(birthDate)
        etGender.setText(gender)
        tvPhoneNumber.text = "Phone: $phoneNumber" // Hiển thị
        tvPassword.text = "Password: ********"        // Hiển thị

        // Xử lý sự kiện click cho các nút Edit
        btnEditPhone.setOnClickListener {
            goToEditPhone()
        }

        btnEditPassword.setOnClickListener {
            goToEditPassword()
        }
    }
    private fun goToEditPhone() {
        val intent = Intent(this, EditPhoneActivity::class.java)
        // Có thể truyền số điện thoại hiện tại sang, nếu cần
        intent.putExtra("CURRENT_PHONE", intent.getStringExtra("PHONE_NUMBER") ?: "123-456-7890")
        startActivity(intent)
    }

    private fun goToEditPassword() {
        val intent = Intent(this, EditPasswordActivity::class.java)
        startActivity(intent)
    }
}