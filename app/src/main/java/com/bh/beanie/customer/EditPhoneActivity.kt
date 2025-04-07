package com.bh.beanie.customer

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bh.beanie.R
import com.google.android.material.textfield.TextInputEditText

class EditPhoneActivity : AppCompatActivity() {

    private lateinit var etNewPhoneNumber: TextInputEditText
    private lateinit var btnSavePhoneNumber: Button
    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_phone)

        etNewPhoneNumber = findViewById(R.id.etNewPhoneNumber)
        btnSavePhoneNumber = findViewById(R.id.btnSavePhoneNumber)
        toolbar = findViewById(R.id.toolbar)

        //Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Lấy số điện thoại hiện tại từ Intent (nếu có)
        val currentPhone = intent.getStringExtra("CURRENT_PHONE")
        etNewPhoneNumber.setText(currentPhone) // Hiển thị để người dùng thấy

        btnSavePhoneNumber.setOnClickListener {
            val newPhoneNumber = etNewPhoneNumber.text.toString().trim()

            if (newPhoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a new phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Gọi API để cập nhật số điện thoại
            Toast.makeText(this,"Phone number updated (not implemented)", Toast.LENGTH_SHORT).show()
            finish()

        }
    }
}