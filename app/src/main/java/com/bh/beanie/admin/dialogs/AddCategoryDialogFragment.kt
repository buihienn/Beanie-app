package com.bh.beanie.admin.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bh.beanie.R

class AddCategoryDialogFragment(
    private val onCategoryCreated: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_category_admin, null)
        val nameEditText = view.findViewById<EditText>(R.id.categoryNameEditText)

        builder.setTitle("Insert Category")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    onCategoryCreated(name)
                } else {
                    Toast.makeText(requireContext(), "Name must not be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }
}