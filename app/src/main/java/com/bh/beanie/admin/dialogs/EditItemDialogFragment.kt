package com.bh.beanie.admin.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bh.beanie.R
import com.bh.beanie.model.Product
import com.bh.beanie.repository.CloudinaryRepository
import com.bh.beanie.repository.FirebaseRepository
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream

class EditItemDialogFragment(
    private val item: Product,
    private val branchId: String,
    private val onItemUpdated: (Product) -> Unit
) : DialogFragment() {

    private lateinit var imageView: ImageView
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private val cloudinaryRepository = CloudinaryRepository()
    private val repository = FirebaseRepository(FirebaseFirestore.getInstance())
    private val sizeEditTextMap = mutableMapOf<String, EditText>()
    private lateinit var progressBar: ProgressBar

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
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imageView)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_edit_item, container, false)

        imageView = view.findViewById(R.id.imageViewItem)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val stockEditText = view.findViewById<EditText>(R.id.editTextStock)
        val saveButton = view.findViewById<Button>(R.id.btnSave)
        val changeImageButton = view.findViewById<Button>(R.id.btnChangeImage)
        val cancelButton = view.findViewById<ImageButton>(R.id.btnDialogCancel)
        val sizeContainer = view.findViewById<LinearLayout>(R.id.sizeContainer)
        val addSizeButton = view.findViewById<Button>(R.id.btnAddSize)
        progressBar = view.findViewById(R.id.progressBar)

        cancelButton.setOnClickListener { dismiss() }

        Glide.with(this)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.ic_launcher_foreground)
            .into(imageView)

        nameEditText.setText(item.name)
        stockEditText.setText(item.stockQuantity.toString())

        item.size.forEach { (size, price) ->
            sizeContainer.addView(createSizeRow(size, price))
        }

        changeImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        addSizeButton.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), addSizeButton)
            listOf("S", "M", "L").forEach { size ->
                popupMenu.menu.add(size)
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedSize = menuItem.title.toString()
                if (sizeEditTextMap.containsKey(selectedSize)) {
                    Toast.makeText(requireContext(), "Size $selectedSize đã tồn tại", Toast.LENGTH_SHORT).show()
                } else {
                    sizeContainer.addView(createSizeRow(selectedSize))
                }
                true
            }

            popupMenu.show()
        }

        saveButton.setOnClickListener {
            val updatedSizes = mutableMapOf<String, Double>()
            sizeEditTextMap.forEach { (size, editText) ->
                val price = editText.text.toString().toDoubleOrNull()
                if (price != null) {
                    updatedSizes[size] = price
                }
            }

            val name = nameEditText.text.toString().trim()
            val stock = stockEditText.text.toString().toIntOrNull()
            if (name.isEmpty() || stock == null || updatedSizes.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedItem = item.copy(
                name = name,
                stockQuantity = stock,
                size = updatedSizes
            )

            selectedImageUri?.let { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    showProgress(true)
                    cloudinaryRepository.uploadImage(
                        filePath = file.absolutePath,
                        folderName = "menu-items",
                        onSuccess = { imageUrl ->
                            val finalItem = updatedItem.copy(imageUrl = imageUrl)
                            updateItemInDatabase(finalItem)
                        },
                        onFailure = { exception ->
                            Log.e("EditItemDialog", "Upload error", exception)
                            Toast.makeText(requireContext(), "Error when upload on Cloudinary", Toast.LENGTH_SHORT).show()
                            showProgress(false)
                        }
                    )
                } else {
                    Toast.makeText(requireContext(), "Error: Can't solve image uploaded", Toast.LENGTH_SHORT).show()
                }
            } ?: updateItemInDatabase(updatedItem)
        }

        return view
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun createSizeRow(size: String, price: Double? = null): View {
        val context = requireContext()
        val rowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        val sizeTextView = TextView(context).apply {
            text = size
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 16 }
        }

        val priceEditText = EditText(context).apply {
            hint = "Enter price"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(price?.toString() ?: "")
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val removeButton = Button(context).apply {
            text = "Xóa"
            setOnClickListener {
                (rowLayout.parent as LinearLayout).removeView(rowLayout)
                sizeEditTextMap.remove(size)
            }
        }

        rowLayout.addView(sizeTextView)
        rowLayout.addView(priceEditText)
        rowLayout.addView(removeButton)
        sizeEditTextMap[size] = priceEditText

        return rowLayout
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
            Log.e("EditItemDialog", "Error converting URI to file", e)
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

    private fun updateItemInDatabase(updatedItem: Product) {
        lifecycleScope.launch {
            try {
                showProgress(true)
                repository.editCategoryItemSuspend(branchId, updatedItem.categoryId, updatedItem)
                Toast.makeText(requireContext(), "Update successful", Toast.LENGTH_SHORT).show()
                onItemUpdated(updatedItem)
                dismiss()
            } catch (exception: Exception) {
                Log.e("EditItemDialog", "Error updating item", exception)
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            } finally {
                showProgress(false)
            }
        }
    }
}
