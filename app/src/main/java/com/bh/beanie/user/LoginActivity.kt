package com.bh.beanie.user

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.admin.AdminMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : AppCompatActivity() {

    // Views
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var txtForgotPassword: TextView
    private lateinit var btnLoginGoogle: Button
    private lateinit var txtSignUpLink: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()
    private val RC_SIGN_IN = 9001

    // Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Map views
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        txtForgotPassword = findViewById(R.id.txtForgotPassword)
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle)
        txtSignUpLink = findViewById(R.id.txtSignUpLink)
        progressBar = findViewById(R.id.loginProgressBar) // Make sure to add this to the layout


        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("610667902405-sqbdn9dsv7krcan8ghufedq8pt67a8pv.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign in button click
        btnSignIn.setOnClickListener {
            attemptLogin()
        }

        // Forgot password link click
        txtForgotPassword.setOnClickListener {
            showForgotPasswordMessage()
        }

        // Google sign in button click
        btnLoginGoogle.setOnClickListener {
            // Sign out first to force account picker
            val googleSignInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build())

            // Sign out from previous Google account
            googleSignInClient.signOut().addOnCompleteListener {
                // After signing out, start the sign in process again with account picker
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            }
        }

        // Sign up link click
        txtSignUpLink.setOnClickListener {
            goToSignUp()
        }
    }

    private fun signInWithGoogle() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed
                showLoading(false)
                Log.w("GoogleSignIn", "Google sign in failed", e)
                Toast.makeText(this, "Google sign-in failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("GoogleSignIn", "firebaseAuthWithGoogle:" + account.id)

        if (account.idToken == null) {
            Log.e("GoogleSignIn", "ID token is null")
            Toast.makeText(this, "Authentication failed: Missing token", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("GoogleSignIn", "signInWithCredential:success")

                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        // New user - DO NOT store the user ID yet
                        // First collect additional info and only persist after successful profile completion
                        checkAndCollectAdditionalInfo(user?.uid)
                    } else {
                        // Existing user with complete profile - store user ID in application for persistence
                        if (user != null) {
                            (application as BeanieApplication).setUserId(user.uid)
                            // Check if user exists in Firestore and navigate accordingly
                            checkUserRoleAndNavigate(user.uid)
                        }
                    }
                } else {
                    Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    // New method to check user role and navigate accordingly

    private fun checkUserRoleAndNavigate(userId: String?) {
        if (userId == null) {
            Log.e("RoleCheck", "User ID is null")
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    val role = document.getString("role") ?: "user"
                    navigateBasedOnRole(role, userId)
                } else {
                    // Create new user document
                    val userMap = hashMapOf(
                        "email" to auth.currentUser?.email,
                        "name" to auth.currentUser?.displayName,
                        "role" to "user",
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            navigateBasedOnRole("user", userId)
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error creating user document", e)
                            Toast.makeText(this, "Error setting up user profile", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.w("Firestore", "Error checking user role", e)
                Toast.makeText(this, "Error checking user role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateBasedOnRole(role: String, userId: String) {
        val currentUser = auth.currentUser
        when (role) {
            "admin" -> {
                val intent = Intent(this, AdminMainActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                finish()
            }
            else -> {
                // For regular users
                val intent = Intent(this, UserMainActivity::class.java)
                intent.putExtra("USER_ID", userId)
                // Make sure these values are not null
                intent.putExtra("USER_EMAIL", currentUser?.email ?: "")
                intent.putExtra("USER_NAME", currentUser?.displayName ?: "")
                Log.d("Navigation", "Sending to UserMain - ID: $userId, Email: ${currentUser?.email}, Name: ${currentUser?.displayName}")
                startActivity(intent)
                finish()
            }
        }
    }

    private fun checkAndCollectAdditionalInfo(userId: String?) {
        if (userId == null) {
            showLoading(false)
            Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user already has a profile in Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists() && document.data != null) {
                    val userData = document.data
                    if (userData?.get("phone") != null &&
                        userData["dob"] != null &&
                        userData["gender"] != null) {
                        // Profile complete - now check role and navigate
                        checkUserRoleAndNavigate(userId)
                    } else {
                        // Missing required fields
                        goToCompleteProfile(userId)
                    }
                } else {
                    // No profile found
                    goToCompleteProfile(userId)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.w("Firestore", "Error checking user profile", e)
                goToCompleteProfile(userId)
            }
    }

    private fun goToCompleteProfile(userId: String) {
        val intent = Intent(this, CompleteProfileActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun attemptLogin() {
        // Reset errors
        edtEmail.error = null
        edtPassword.error = null

        // Get values
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            edtPassword.error = getString(R.string.error_field_required)
            focusView = edtPassword
            cancel = true
        }

        // Check for a valid email address
        if (TextUtils.isEmpty(email)) {
            edtEmail.error = getString(R.string.error_field_required)
            focusView = edtEmail
            cancel = true
        } else if (!isEmailValid(email)) {
            edtEmail.error = getString(R.string.error_invalid_email)
            focusView = edtEmail
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and perform the user login attempt
            showLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success
                        val user = auth.currentUser
                        if (user != null) {
                            // User is signed in
                            (application as BeanieApplication).setUserId(user.uid)
                        }
                        Log.d("LOGIN", "signInWithEmail:success")
                        showLoading(false)
                        checkUserRoleAndNavigate(auth.currentUser?.uid)
                    } else {
                        // Sign in failed
                        Log.w("LOGIN", "signInWithEmail:failure", task.exception)
                        showLoading(false)

                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthInvalidUserException) {
                            // No user found with this email
                            edtEmail.error = getString(R.string.error_email_not_found)
                            edtEmail.requestFocus()
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            // Wrong password
                            edtPassword.error = getString(R.string.error_incorrect_password)
                            edtPassword.requestFocus()
                        } catch (e: Exception) {
                            // Other errors
                            Toast.makeText(baseContext, "Login failed: ${e.localizedMessage}",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    // Check if email is valid
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Show/hide loading spinner
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Instead of just disabling buttons, use alpha to indicate they're disabled
        // This prevents layout changes
        val alpha = if (isLoading) 0.5f else 1.0f
        btnSignIn.isEnabled = !isLoading
        btnSignIn.alpha = alpha
        btnLoginGoogle.isEnabled = !isLoading
        btnLoginGoogle.alpha = alpha
        txtForgotPassword.isEnabled = !isLoading
        txtSignUpLink.isEnabled = !isLoading
    }

    private fun showForgotPasswordMessage() {
        Toast.makeText(this, "Forgot password feature is coming soon.", Toast.LENGTH_SHORT).show()
    }


    private fun goToSignUp() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }

}