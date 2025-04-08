package com.bh.beanie.customer

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.R
import java.util.*

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)  // Đổi layout tương ứng

        val dobEditText = findViewById<EditText>(R.id.dobEditText)
        val genderSpinner = findViewById<Spinner>(R.id.genderSpinner)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        val genderOptions = listOf("Male", "Female", "Other")
        genderSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)

        dobEditText.setOnClickListener {
            val c = Calendar.getInstance()
            val dp = DatePickerDialog(this, { _, y, m, d ->
                dobEditText.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            dp.show()
        }

        signUpButton.setOnClickListener {
            val pass = passwordEditText.text.toString()
            val confirm = confirmPasswordEditText.text.toString()

            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                // TODO: Submit to backend
            }
        }
    }
}