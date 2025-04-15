package com.bh.beanie

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.admin.AdminMainActivity
import com.bh.beanie.customer.LoginActivity
import com.bh.beanie.user.UserMainActivity
import com.bh.beanie.utils.NavigationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null)
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // No user is signed in, go to login screen
            NavigationUtils.navigateToLogin(this)
        } else {
            // User is signed in, check role and navigate
            NavigationUtils.navigateBasedOnRole(this, currentUser.uid)
        }
    }
}