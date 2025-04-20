package com.bh.beanie.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bh.beanie.R
import com.bh.beanie.utils.LuckyWheelView

class LuckyWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val wheelImageView: ImageView
    private val textViewList = mutableListOf<TextView>()
    private val segments = 8

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_lucky_wheel, this, true)

        // Get wheel image
        wheelImageView = findViewById(R.id.wheelImageView)

        // Create text views for segments
        for (i in 0 until segments) {
            val textView = findViewById<TextView>(
                resources.getIdentifier("segmentText$i", "id", context.packageName)
            )
            textViewList.add(textView)
        }
    }

    fun setVoucherTexts(voucherTexts: List<String>) {
        val textsToUse = if (voucherTexts.size >= segments) {
            voucherTexts.take(segments)
        } else {
            // Repeat the list to fill all segments
            voucherTexts + List(segments - voucherTexts.size) {
                voucherTexts[it % voucherTexts.size]
            }
        }

        // Set text for each segment
        for (i in 0 until segments) {
            textViewList[i].text = textsToUse[i]
        }
    }

    fun spin(degreesToRotate: Float, duration: Long, onEnd: () -> Unit) {
        val rotateAnimation = ObjectAnimator.ofFloat(
            this, "rotation", 0f, degreesToRotate
        ).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
        }
        rotateAnimation.start()
    }
}