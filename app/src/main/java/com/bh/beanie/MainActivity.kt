package com.bh.beanie

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.admin.AdminMainActivity
import com.bh.beanie.user.UserMainActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnTest = findViewById<Button>(R.id.btnTest)
        btnTest.setOnClickListener {
            val intent = Intent (this, AdminMainActivity::class.java)
            startActivity(intent)
        }

        val btnTestUser = findViewById<Button>(R.id.btnTestUser)
        btnTestUser.setOnClickListener {
            val intent = Intent (this, UserMainActivity::class.java)
            startActivity(intent)
        }
    }
}