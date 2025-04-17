package com.bh.beanie.admin.dialogs

import android.app.Activity
import android.content.Intent
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
import com.bh.beanie.model.Category
import com.bh.beanie.model.CategoryItem
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
    private val onItemAdded: (CategoryItem) -> Unit
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
        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)
        val stockEditText = view.findViewById<EditText>(R.id.editTextStock)
        val saveButton = view.findViewById<Button>(R.id.btnSave)
        val changeImageButton = view.findViewById<Button>(R.id.btnChooseImgAdmin)
        val cancelButton = view.findViewById<ImageButton>(R.id.btnDialogExit)
        categorySpinner = view.findViewById(R.id.spinnerCategory)

        cancelButton.setOnClickListener { dismiss() }

        changeImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        saveButton.setOnClickListener {
            val selectedCategory = categorySpinner.selectedItemPosition
            if (selectedCategory != Spinner.INVALID_POSITION && selectedCategory < categories.size) {
                val category = categories[selectedCategory]

                val newItem = CategoryItem(
                    id = UUID.randomUUID().toString(),
                    name = nameEditText.text.toString(),
                    price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    stockQuantity = stockEditText.text.toString().toIntOrNull() ?: 0,
                    imageUrl = "",
                    description = "",
                    categoryId = category.id
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
                        Toast.makeText(requireContext(), "Cannot process selected image", Toast.LENGTH_SHORT).show()
                    }
                } ?: addItemToDatabase(newItem)
            } else {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
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

    private fun addItemToDatabase(item: CategoryItem) {
        lifecycleScope.launch {
            try {
                repository.addCategoryItemSuspend(branchId, item.categoryId, item)
                onItemAdded(item)
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
}
