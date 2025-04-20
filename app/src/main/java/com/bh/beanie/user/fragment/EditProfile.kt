package com.bh.beanie.user.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.User
import com.bh.beanie.repository.UserRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfile : Fragment() {
    // View references
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvProfileTitle: TextView
    private lateinit var ivProfilePic: ShapeableImageView
    private lateinit var tvUserName: TextView
    private lateinit var etEmailDisabled1: TextInputEditText
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnUpdateProfile: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Repository reference
    private lateinit var userRepository: UserRepository
    private lateinit var auth: FirebaseAuth
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize repository
        userRepository = UserRepository.getInstance()

        // Map views
        mapViews(view)

        // Setup button click listeners
        setupButtonClickListeners()

        // Load user data
        loadUserData()
    }

    private fun mapViews(view: View) {
        ivBackArrow = view.findViewById(R.id.ivBackArrow)
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        tvUserName = view.findViewById(R.id.tvUserName)
        etEmailDisabled1 = view.findViewById(R.id.etEmailDisabled1)
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun loadUserData() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                currentUser = userRepository.getCurrentUser()
                currentUser?.let { user ->
                    updateUI(user)
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateUI(user: User) {
        tvUserName.text = user.username
        etEmailDisabled1.setText(user.email)
        etPhoneNumber.setText(user.phone)
    }

    private fun setupButtonClickListeners() {
        ivBackArrow.setOnClickListener {
            requireActivity().onBackPressed()
        }

        btnUpdateProfile.setOnClickListener {
            updateProfile()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateProfile() {
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validate phone number
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.error = "Phone number cannot be empty"
            return
        }

        // Validate password fields if they're not empty
        if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            if (newPassword.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return
            }

            if (newPassword != confirmPassword) {
                etConfirmPassword.error = "Passwords don't match"
                return
            }
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Update phone number in Firestore
                currentUser?.let { user ->
                    user.phone = phoneNumber
                    val profileUpdated = userRepository.updateUserProfile(user)

                    // Update password in Firebase Auth if provided
                    var passwordUpdated = true
                    if (newPassword.isNotEmpty()) {
                        passwordUpdated = updateUserPassword(newPassword)
                    }

                    if (profileUpdated && passwordUpdated) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        // Clear password fields
                        etNewPassword.text?.clear()
                        etConfirmPassword.text?.clear()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update some profile information", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "No user data available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun updateUserPassword(newPassword: String): Boolean {
        return try {
            auth.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val success = userRepository.deleteAccount()
                if (success) {
                    Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnUpdateProfile.isEnabled = false
            btnDeleteAccount.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnUpdateProfile.isEnabled = true
            btnDeleteAccount.isEnabled = true
        }
    }

    companion object {
        fun newInstance() = EditProfile()
    }
}