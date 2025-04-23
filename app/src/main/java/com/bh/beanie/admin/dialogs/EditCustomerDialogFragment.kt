package com.bh.beanie.admin.dialogs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.User
import com.bh.beanie.repository.FirebaseRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class EditCustomerDialogFragment(
    private val user: User,
    private val onCustomerUpdated: (User) -> Unit
) : DialogFragment() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etGender: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: ImageButton

    private val firebaseRepository = FirebaseRepository(FirebaseFirestore.getInstance())

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_edit_customer_admin, container, false)

        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etBirthDate = view.findViewById(R.id.etBirthDate)
        etGender = view.findViewById(R.id.etGender)
        etPhone = view.findViewById(R.id.etPhone)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Pre-fill fields with current user data
        etFullName.setText(user.username)
        etEmail.setText(user.email)
        etBirthDate.setText(user.dob)
        etGender.setText(user.gender)
        etPhone.setText(user.phone)

        // Handle Save button click
        btnSave.setOnClickListener {
            val updatedUser = user.copy(
                username = etFullName.text.toString().trim(),
                email = etEmail.text.toString().trim(),
                dob = etBirthDate.text.toString().trim(),
                gender = etGender.text.toString().trim(),
                phone = etPhone.text.toString().trim()
            )

            if (validateFields(updatedUser)) {
                updateCustomerInDatabase(updatedUser)
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Cancel button click
        btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun validateFields(user: User): Boolean {
        return user.username.isNotEmpty() &&
                user.email.isNotEmpty() &&
                user.dob.isNotEmpty() &&
                user.gender.isNotEmpty() &&
                user.phone.isNotEmpty()
    }

    private fun updateCustomerInDatabase(updatedUser: User) {
        lifecycleScope.launch {
            try {
                firebaseRepository.updateCustomer(updatedUser)
                onCustomerUpdated(updatedUser)
                Toast.makeText(requireContext(), "Customer updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e("EditCustomerDialog", "Error updating customer", e)
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}