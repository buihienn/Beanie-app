package com.bh.beanie.admin.dialogs

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.bh.beanie.repository.CloudinaryRepository
import com.bh.beanie.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddVoucherDialogFragment(
    private val onVoucherAdded: (Voucher) -> Unit
) : DialogFragment() {

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView
    private val cloudinaryRepository = CloudinaryRepository()
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
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_voucher_admin, container, false)

        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextContent = view.findViewById<EditText>(R.id.editTextContent)
        val spinnerDiscountType = view.findViewById<Spinner>(R.id.spinnerDiscountType)
        val editTextValue = view.findViewById<EditText>(R.id.editTextValue)
        val editTextExpiryDate = view.findViewById<EditText>(R.id.editTextExpiryDate)
        val btnChooseImg = view.findViewById<Button>(R.id.btnChooseImgAdmin)
        val btnCreate = view.findViewById<Button>(R.id.btnCreate)
        val imgCancel = view.findViewById<ImageButton>(R.id.imgBtnCancel)
        imageView = view.findViewById(R.id.imageViewVoucher) // ImageView for displaying selected image

        val discountTypeList = listOf("PERCENT", "FIXED")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            discountTypeList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiscountType.adapter = adapter

        // Set DatePicker
        editTextExpiryDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    editTextExpiryDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Handle Cancel
        imgCancel.setOnClickListener {
            dismiss()
        }

        // Launch image picker when button clicked
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(imageView)
            }
        }

        btnChooseImg.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Handle Create Voucher
        btnCreate.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val type = spinnerDiscountType.selectedItem?.toString() ?: ""
            val value = editTextValue.text.toString().toDoubleOrNull()
            val expiryDateStr = editTextExpiryDate.text.toString().trim()

            if (name.isEmpty() || content.isEmpty() || value == null || expiryDateStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr)
            val expiryTimestamp = Timestamp(expiryDate!!)

            val voucher = Voucher(
                id = "", // Firebase sẽ tự tạo
                name = name,
                content = content,
                expiryDate = expiryTimestamp,
                state = "ACTIVE",
                imageUrl = "",  // Default image URL to be replaced after upload
                discountType = type,
                discountValue = value,
                minOrderAmount = 0.0
            )

            selectedImageUri?.let { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    cloudinaryRepository.uploadImage(
                        filePath = file.absolutePath,
                        folderName = "vouchers",
                        onSuccess = { imageUrl ->
                            val updatedVoucher = voucher.copy(imageUrl = imageUrl)
                            addVoucherToDatabase(updatedVoucher)
                        },
                        onFailure = { exception ->
                            Log.e("AddVoucherDialog", "Upload error", exception)
                            Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(requireContext(), "Cannot process selected image", Toast.LENGTH_SHORT).show()
                }
            } ?: addVoucherToDatabase(voucher) // Add voucher without image if no image selected
        }

        return view
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
            Log.e("AddVoucherDialog", "Error converting URI to file", e)
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

    private fun addVoucherToDatabase(voucher: Voucher) {
        lifecycleScope.launch {
            try {
                firebaseRepository.addVoucherSuspend(voucher)
                onVoucherAdded(voucher)
                Toast.makeText(requireContext(), "Create voucher successful", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (exception: Exception) {
                Log.e("AddVoucherDialog", "Error adding voucher", exception)
                Toast.makeText(requireContext(), "Add failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
