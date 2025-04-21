package com.bh.beanie.user.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.bh.beanie.R
import com.bh.beanie.customer.LoginActivity
import com.bh.beanie.utils.NavigationUtils.logout
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

// Define constants for this fragment specifically
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OtherFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_other, container, false)

        // Set up click listeners for the cards
        setupCardViewListeners(view)

        return view
    }

    private fun setupCardViewListeners(view: View) {
        // Profile card
        view.findViewById<MaterialCardView>(R.id.cardProfile)?.setOnClickListener {
            // Handle profile click
        }

        // Settings card
        view.findViewById<MaterialCardView>(R.id.cardSettings)?.setOnClickListener {
            // Handle settings click
        }

        // Order History card
        view.findViewById<MaterialCardView>(R.id.cardOrderHistory)?.setOnClickListener {
            // Handle order history click
        }

        // Terms & Conditions card
        view.findViewById<MaterialCardView>(R.id.cardTermsConditions)?.setOnClickListener {
            // Handle terms & conditions click
        }

        // Add logout button click listener - using Button instead of MaterialCardView
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            activity?.let { currentActivity ->
                logout(currentActivity)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OtherFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}