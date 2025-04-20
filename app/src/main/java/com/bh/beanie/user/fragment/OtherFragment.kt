package com.bh.beanie.user.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.bh.beanie.R
import com.bh.beanie.customer.LoginActivity
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
            // Navigate to EditProfile fragment
            val editProfileFragment = EditProfile.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editProfileFragment) // Ensure this ID matches your container
                .addToBackStack(null) // So user can press back to return to previous fragment
                .commit()
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
            logoutUser()
        }
    }

    private fun logoutUser() {
        // Get Firebase Auth instance
        val auth = FirebaseAuth.getInstance()

        // Sign out the user
        auth.signOut()

        // Show logout message
        Toast.makeText(requireContext(), "You have been logged out", Toast.LENGTH_SHORT).show()

        // Navigate to login screen
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
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