package com.bh.beanie.user

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bh.beanie.MainActivity
import com.bh.beanie.R
import com.bh.beanie.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CompleteProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var completeProfileButton: Button
    private lateinit var progressBar: ProgressBar

    // Firebase
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    // User ID from intent
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Map views
        usernameEditText = findViewById(R.id.usernameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        dobEditText = findViewById(R.id.dobEditText)
        genderSpinner = findViewById(R.id.genderSpinner)
        completeProfileButton = findViewById(R.id.completeProfileButton)
        progressBar = findViewById(R.id.progressBar)

        // Set up gender spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.gender_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = adapter
        }

        // Set up DOB date picker
        setupDatePicker()

        // Pre-fill username if available from Firebase Auth
        auth.currentUser?.let { user ->
            user.displayName?.let { displayName ->
                usernameEditText.setText(displayName)
            }
        }

        // Complete profile button click
        completeProfileButton.setOnClickListener {
            completeProfile()
        }
    }

    private fun setupDatePicker() {
        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Set default date to 18 years ago
            calendar.add(Calendar.YEAR, -18)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    calendar.set(year, monthOfYear, dayOfMonth)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dobEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set max date to today
            val today = Calendar.getInstance()
            datePickerDialog.datePicker.maxDate = today.timeInMillis

            datePickerDialog.show()
        }
    }

    private fun completeProfile() {
        // Reset errors
        usernameEditText.error = null
        phoneEditText.error = null
        dobEditText.error = null

        // Get values
        val username = usernameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val dob = dobEditText.text.toString().trim()
        val selectedGenderPosition = genderSpinner.selectedItemPosition
        val selectedGender = if (selectedGenderPosition > 0) genderSpinner.selectedItem.toString() else ""

        var cancel = false
        var focusView: View? = null

        // Gender
        if (selectedGenderPosition == 0) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
            focusView = genderSpinner
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

        // Username
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = getString(R.string.error_field_required)
            focusView = usernameEditText
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            // Validation OK -> Save info
            saveUserInfo(username, phone, dob, selectedGender)
        }
    }

    private fun saveUserInfo(username: String, phone: String, dob: String, gender: String) {
        showLoading(true)

        // Get email from Firebase Auth
        val email = auth.currentUser?.email ?: ""

        // Create User object
        val userProfile = User(
            username = username,
            email = email,
            phone = phone,
            dob = dob,
            gender = gender
        )

        // Save to Firestore
        db.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                Log.d("CompleteProfile", "User profile successfully saved!")
                showLoading(false)
                goToMainActivity()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.w("CompleteProfile", "Error saving user profile", e)
                Toast.makeText(this, "Failed to save profile: ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        completeProfileButton.isEnabled = !isLoading
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}