package com.bh.beanie

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.user.LoginActivity
import com.bh.beanie.utils.NavigationUtils
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("LanguageSettings", MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null)
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d("DEBUG", "User is signed in: ${currentUser.email}")
            // User is already signed in
            val userId = currentUser.uid  // Use currentUser directly since we know it's not null

            // Store user ID in application for persistence
            (application as BeanieApplication).setUserId(userId)

            // Navigate based on stored role - pass the non-null userId
            NavigationUtils.navigateBasedOnRole(this, userId)
        } else {
            // No user signed in, go to login

            Log.d("DEBUG", "No user signed in, navigating to LoginActivity")


            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}