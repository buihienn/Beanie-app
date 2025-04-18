package com.bh.beanie.admin.dialogs

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bumptech.glide.Glide
import com.bh.beanie.repository.CloudinaryRepository
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditVoucherDialogFragment(
    private val voucher: Voucher,
    private val onVoucherUpdated: (Voucher) -> Unit
) : DialogFragment() {

    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView
    private val cloudinaryRepository = CloudinaryRepository()
    private val firebaseRepository = FirebaseRepository(FirebaseFirestore.getInstance())

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                Glide.with(this).load(it).into(imageView)
            }
        }
    }

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
        val view = inflater.inflate(R.layout.dialog_edit_voucher_admin, container, false)

        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextContent = view.findViewById<EditText>(R.id.editTextContent)
        val spinnerDiscountType = view.findViewById<Spinner>(R.id.spinnerDiscountType)
        val editTextValue = view.findViewById<EditText>(R.id.editTextValue)
        val editTextExpiryDate = view.findViewById<EditText>(R.id.editTextExpiryDate)
        val spinnerState = view.findViewById<Spinner>(R.id.spinnerState)
        val btnChooseImg = view.findViewById<Button>(R.id.btnChooseImgAdmin)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val imgCancel = view.findViewById<ImageButton>(R.id.imgBtnCancel)
        imageView = view.findViewById(R.id.imageViewVoucher)

        // Pre-fill fields with current voucher data
        editTextName.setText(voucher.name)
        editTextContent.setText(voucher.content)
        editTextValue.setText(voucher.discountValue.toString())
        editTextExpiryDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(voucher.expiryDate.toDate()))
        Glide.with(this).load(voucher.imageUrl).placeholder(R.drawable.placeholder).into(imageView)

        val discountTypeList = listOf("PERCENT", "FIXED")
        val discountTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, discountTypeList)
        discountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiscountType.adapter = discountTypeAdapter
        spinnerDiscountType.setSelection(discountTypeList.indexOf(voucher.discountType))

        val stateList = listOf("ACTIVE", "DISABLED", "EXPIRED")
        val stateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stateList)
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerState.adapter = stateAdapter
        spinnerState.setSelection(stateList.indexOf(voucher.state))

        // Date picker
        editTextExpiryDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = voucher.expiryDate.toDate()
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                editTextExpiryDate.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        // Cancel button
        imgCancel.setOnClickListener { dismiss() }

        // Image picker
        btnChooseImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Save
        btnSave.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val type = spinnerDiscountType.selectedItem?.toString() ?: ""
            val value = editTextValue.text.toString().toDoubleOrNull()
            val expiryDateStr = editTextExpiryDate.text.toString().trim()
            val state = spinnerState.selectedItem?.toString() ?: ""

            if (name.isEmpty() || content.isEmpty() || value == null || expiryDateStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr)
            val expiryTimestamp = Timestamp(expiryDate!!)

            val updatedVoucher = voucher.copy(
                name = name,
                content = content,
                discountType = type,
                discountValue = value,
                expiryDate = expiryTimestamp,
                state = state
            )

            selectedImageUri?.let { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    cloudinaryRepository.uploadImage(
                        filePath = file.absolutePath, // ✅ Dùng đường dẫn file thực
                        folderName = "vouchers",
                        onSuccess = { imageUrl ->
                            val voucherWithImage = updatedVoucher.copy(imageUrl = imageUrl)
                            updateVoucherInDatabase(voucherWithImage)
                        },
                        onFailure = {
                            Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(requireContext(), "Cannot read selected image", Toast.LENGTH_SHORT).show()
                }
            } ?: updateVoucherInDatabase(updatedVoucher)
        }

        return view
    }

    private fun updateVoucherInDatabase(voucher: Voucher) {
        lifecycleScope.launch {
            try {
                firebaseRepository.addVoucherSuspend(voucher)
                onVoucherUpdated(voucher)
                Toast.makeText(requireContext(), "Voucher updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e("EditVoucherDialog", "Error updating voucher", e)
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Convert URI to file
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileNameFromUri(uri) ?: "temp_image.jpg"
            val tempFile = File.createTempFile("upload_", fileName, requireContext().cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("EditVoucherDialog", "Error converting URI to file", e)
            null
        }
    }

    // Get the file name from URI
    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                name = it.getString(index)
            }
        }
        return name
    }
}
