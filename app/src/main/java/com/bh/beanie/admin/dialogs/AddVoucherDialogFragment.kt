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
import com.bh.beanie.repository.CloudinaryRepository
import com.bh.beanie.repository.FirebaseRepository
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
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
    private lateinit var imageViewVoucher: ImageView

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
    ): View {
        val view = inflater.inflate(R.layout.dialog_add_voucher_admin, container, false)
        setupUI(view)
        return view
    }

    private fun setupUI(view: View) {
        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextContent = view.findViewById<EditText>(R.id.editTextContent)
        val spinnerDiscountType = view.findViewById<Spinner>(R.id.spinnerDiscountType)
        val editTextValue = view.findViewById<EditText>(R.id.editTextValue)
        val editTextExpiryDate = view.findViewById<EditText>(R.id.editTextExpiryDate)
        val btnChooseImage = view.findViewById<Button>(R.id.btnChooseImgAdmin)
        val btnCreate = view.findViewById<Button>(R.id.btnCreate)
        val imgBtnCancel = view.findViewById<ImageButton>(R.id.imgBtnCancel)
        val radioGroupLevels = view.findViewById<RadioGroup>(R.id.radioGroupLevels)
        imageViewVoucher = view.findViewById(R.id.imageViewVoucher)

        // Setup spinner DiscountType
        val discountTypes = listOf("PERCENT", "FIXED")
        spinnerDiscountType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            discountTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Setup date picker
        editTextExpiryDate.setOnClickListener {
            showDatePicker(editTextExpiryDate)
        }

        // Setup cancel button
        imgBtnCancel.setOnClickListener {
            dismiss()
        }

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(imageViewVoucher)
            }
        }
        btnChooseImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Handle Create Voucher
        btnCreate.setOnClickListener {
            createVoucher(
                editTextName.text.toString().trim(),
                editTextContent.text.toString().trim(),
                spinnerDiscountType.selectedItem?.toString() ?: "",
                editTextValue.text.toString().toDoubleOrNull(),
                editTextExpiryDate.text.toString().trim(),
                getSelectedLevel(radioGroupLevels)
            )
        }
    }

    private fun showDatePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                targetEditText.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun getSelectedLevel(radioGroup: RadioGroup): String? {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.radioButtonAll -> "All"
            R.id.radioButtonNew -> "New"
            R.id.radioButtonLoyal -> "Loyal"
            R.id.radioButtonVIP -> "VIP"
            else -> null
        }
    }

    private fun createVoucher(
        name: String,
        content: String,
        discountType: String,
        discountValue: Double?,
        expiryDateStr: String,
        level: String?
    ) {
        if (level == null) {
            Toast.makeText(requireContext(), "Please select a level", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isEmpty() || content.isEmpty() || discountValue == null || expiryDateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val expiryTimestamp = try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Timestamp(sdf.parse(expiryDateStr)!!)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ngày hết hạn không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        val voucher = Voucher(
            id = FirebaseFirestore.getInstance().collection("vouchers").document().id, // Generate random ID
            name = name,
            content = content,
            expiryDate = expiryTimestamp,
            state = "ACTIVE",
            imageUrl = "",
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = 0.0
        )

        selectedImageUri?.let { uri ->
            uriToFile(uri)?.let { file ->
                uploadImageAndAddVoucher(file, voucher)
            } ?: run {
                Toast.makeText(requireContext(), "Không thể xử lý ảnh đã chọn", Toast.LENGTH_SHORT).show()
            }
        } ?: addVoucherToDatabase(voucher) // Nếu không chọn ảnh
    }

    private fun uploadImageAndAddVoucher(file: File, voucher: Voucher) {
        cloudinaryRepository.uploadImage(
            filePath = file.absolutePath,
            folderName = "vouchers",
            onSuccess = { imageUrl ->
                val updatedVoucher = voucher.copy(imageUrl = imageUrl)
                addVoucherToDatabase(updatedVoucher)
            },
            onFailure = { exception ->
                Log.e("AddVoucherDialog", "Upload error", exception)
                Toast.makeText(requireContext(), "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun addVoucherToDatabase(voucher: Voucher) {
        lifecycleScope.launch {
            try {
                // Add the voucher to the database
                firebaseRepository.addVoucherSuspend(voucher)

                // Generate user vouchers for the selected level
                val selectedLevel = getSelectedLevel(requireView().findViewById(R.id.radioGroupLevels))
                if (selectedLevel != null) {
                    firebaseRepository.createUserVouchersForLevel(selectedLevel, voucher.id)
                }

                // Notify the parent component and dismiss the dialog
                onVoucherAdded(voucher)
                Toast.makeText(requireContext(), "Voucher created successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e("AddVoucherDialog", "Error adding voucher", e)
                Toast.makeText(requireContext(), "Failed to create voucher", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) it.getString(nameIndex) else null
        }
    }
}
