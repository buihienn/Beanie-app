package com.bh.beanie.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import com.bh.beanie.R
import com.bh.beanie.model.OrderRating
import com.bh.beanie.repository.RatingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RateOrderDialog(
    context: Context,
    private val orderId: String,
    private val existingRating: OrderRating? = null,
    private val onRatingSubmitted: (Boolean) -> Unit
) : Dialog(context) {

    private val ratingRepository = RatingRepository()
    private lateinit var ratingBar: RatingBar
    private lateinit var commentEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rate_order)

        // Initialize views
        ratingBar = findViewById(R.id.ratingBar)
        commentEditText = findViewById(R.id.commentEditText)
        submitButton = findViewById(R.id.submitButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Set existing rating if available
        existingRating?.let {
            ratingBar.rating = it.rating.toFloat()
            commentEditText.setText(it.comment)
        }

        // Set up listeners
        cancelButton.setOnClickListener {
            dismiss()
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = commentEditText.text.toString().trim()

            if (rating <= 0) {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                submitButton.isEnabled = false
                val success = withContext(Dispatchers.IO) {
                    ratingRepository.rateOrder(orderId, rating, comment)
                }

                if (success) {
                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    onRatingSubmitted(true)
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to submit rating. Please try again.", Toast.LENGTH_SHORT).show()
                    submitButton.isEnabled = true
                }
            }
        }
    }
}