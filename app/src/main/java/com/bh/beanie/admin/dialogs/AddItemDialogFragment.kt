package com.bh.beanie.admin.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.Category
import com.bh.beanie.model.Product
import com.bh.beanie.repository.CloudinaryRepository
import com.bh.beanie.repository.FirebaseRepository
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AddItemDialogFragment(
    private val branchId: String,
    private val onItemAdded: (Product) -> Unit
) : DialogFragment() {

    private lateinit var imageView: ImageView
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private lateinit var categorySpinner: Spinner
    private val cloudinaryRepository = CloudinaryRepository()
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())

    private var categories: List<Category> = emptyList()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    Glide.with(this)
                        .load(it)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(imageView)
                }
            }
        }

        lifecycleScope.launch {
            try {
                categories = repository.fetchCategoriesSuspend(branchId)
                updateCategorySpinner()
            } catch (e: Exception) {
                Log.e("AddItemDialog", "Error fetching categories", e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_add_item_admin, container, false)

        imageView = view.findViewById(R.id.imageViewItem)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
//        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)
        val sizeExtraContainer = view.findViewById<LinearLayout>(R.id.sizeExtraContainer)
        val btnAddSize = view.findViewById<Button>(R.id.btnAddSize)
        val stockEditText = view.findViewById<EditText>(R.id.editTextStock)
        val saveButton = view.findViewById<Button>(R.id.btnSave)
        val changeImageButton = view.findViewById<Button>(R.id.btnChooseImgAdmin)
        val cancelButton = view.findViewById<ImageButton>(R.id.btnDialogExit)
        categorySpinner = view.findViewById(R.id.spinnerCategory)


        val addedSizes = mutableSetOf<String>()

        cancelButton.setOnClickListener { dismiss() }

        changeImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        btnAddSize.setOnClickListener {
            val options = arrayOf("S", "L")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Choose a size to add")
            builder.setItems(options) { _, which ->
                val size = options[which]
                if (addedSizes.contains(size)) {
                    Toast.makeText(context, "Size $size already added", Toast.LENGTH_SHORT).show()
                } else {
                    addedSizes.add(size)

                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 8, 0, 0)
                    }

                    val label = TextView(requireContext()).apply {
                        text = "$size:"
                        setPadding(0, 0, 16, 0)
                    }

                    val input = EditText(requireContext()).apply {
                        hint = "Enter price"
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        tag = "size_$size" // để sau lấy data dễ
                    }

                    row.addView(label)
                    row.addView(input)
                    sizeExtraContainer.addView(row)
                }
            }
            builder.show()
        }


        saveButton.setOnClickListener {
            var isValid = true

            // Validate các trường cơ bản
            if (isEmptyField(nameEditText, "Name can't be null")) isValid = false
            if (isEmptyField(stockEditText, "Stock can't be null")) isValid = false
            else if (stockEditText.text.toString().trim().toIntOrNull() == null) {
                stockEditText.error = "Stock must be a number"
                isValid = false
            }

            // Validate size M
            val sizeMEditText = view.findViewById<EditText>(R.id.editTextSizeM)
            if (isEmptyField(sizeMEditText, "Please input price for size M")) isValid = false
            else if (sizeMEditText.text.toString().trim().toDoubleOrNull() == null) {
                sizeMEditText.error = "Price must be a number"
                isValid = false
            }

            // Validate các size động (S, L, ...)
            for (i in 0 until sizeExtraContainer.childCount) {
                val row = sizeExtraContainer.getChildAt(i) as LinearLayout
                val input = row.getChildAt(1) as EditText
                if (isEmptyField(input, "Please input price for size")) isValid = false
                else if (input.text.toString().trim().toDoubleOrNull() == null) {
                    input.error = "Price must be a number"
                    isValid = false
                }
            }

            if (!isValid) return@setOnClickListener

            val sizeMap = mutableMapOf<String, Double>()

            val sizeMPrice = sizeMEditText.text.toString().toDoubleOrNull()
            if (sizeMPrice != null) {
                sizeMap["M"] = sizeMPrice
            }

            for (i in 0 until sizeExtraContainer.childCount) {
                val row = sizeExtraContainer.getChildAt(i) as LinearLayout
                val label = row.getChildAt(0) as TextView
                val input = row.getChildAt(1) as EditText

                val size = label.text.toString().removeSuffix(":").trim()
                val price = input.text.toString().toDoubleOrNull()
                if (price != null) {
                    sizeMap[size] = price
                }
            }

            val selectedCategory = categorySpinner.selectedItemPosition
            if (selectedCategory != Spinner.INVALID_POSITION && selectedCategory < categories.size) {
                val category = categories[selectedCategory]

                val defaultPrice = sizeMap["M"] ?: sizeMap.values.firstOrNull() ?: 0.0

                val newItem = Product(
                    id = UUID.randomUUID().toString(),
                    name = nameEditText.text.toString().trim(),
                    price = defaultPrice,
                    stockQuantity = stockEditText.text.toString().trim().toInt(),
                    imageUrl = "",
                    description = "",
                    categoryId = category.id,
                    size = sizeMap,
                    toppingsAvailable = emptyList()
                )

                selectedImageUri?.let { uri ->
                    val file = uriToFile(uri)
                    if (file != null) {
                        cloudinaryRepository.uploadImage(
                            filePath = file.absolutePath,
                            folderName = "menu-items",
                            onSuccess = { imageUrl ->
                                val finalItem = newItem.copy(imageUrl = imageUrl)
                                addItemToDatabase(finalItem)
                            },
                            onFailure = { exception ->
                                Log.e("AddItemDialog", "Upload error", exception)
                                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(requireContext(), "Không xử lý được ảnh đã chọn", Toast.LENGTH_SHORT).show()
                    }
                } ?: addItemToDatabase(newItem)
            } else {
                Toast.makeText(requireContext(), "Please select category", Toast.LENGTH_SHORT).show()
            }
        }




        return view
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
            Log.e("AddItemDialog", "Error converting URI to file", e)
            null
        }
    }

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

    private fun addItemToDatabase(item: Product) {
        lifecycleScope.launch {
            try {
                repository.addCategoryItemSuspend(branchId, item.categoryId, item)
                onItemAdded(item)
                Toast.makeText(requireContext(), "Add successful", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (exception: Exception) {
                Log.e("AddItemDialog", "Error adding item", exception)
                Toast.makeText(requireContext(), "Add failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun isEmptyField(editText: EditText, errorMsg: String): Boolean {
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) {
            editText.error = errorMsg
            true
        } else {
            false
        }
    }
}
