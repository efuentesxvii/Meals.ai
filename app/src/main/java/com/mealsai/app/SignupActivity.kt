package com.mealsai.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mealsai.app.model.User
import com.mealsai.app.services.FirebaseAuthService
import com.mealsai.app.services.FirestoreService
import com.mealsai.app.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {
    
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etAge: EditText
    private lateinit var etDobDay: EditText
    private lateinit var etDobMonth: EditText
    private lateinit var etDobYear: EditText
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var btnCreateAccount: MaterialButton
    private lateinit var tvLoginLink: TextView
    
    private val preferenceManager by lazy { PreferenceManager.getInstance(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etAge = findViewById(R.id.etAge)
        etDobDay = findViewById(R.id.etDobDay)
        etDobMonth = findViewById(R.id.etDobMonth)
        etDobYear = findViewById(R.id.etDobYear)
        etHeight = findViewById(R.id.etHeight)
        etWeight = findViewById(R.id.etWeight)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }
    
    private fun setupClickListeners() {
        findViewById<TextView>(R.id.tvTopLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        btnCreateAccount.setOnClickListener {
            handleSignup()
        }
        
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun handleSignup() {
        val name = etName.text?.toString()?.trim() ?: ""
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val age = etAge.text?.toString()?.toIntOrNull() ?: 0
        val dobDay = etDobDay.text?.toString()?.trim() ?: ""
        val dobMonth = etDobMonth.text?.toString()?.trim() ?: ""
        val dobYear = etDobYear.text?.toString()?.trim() ?: ""
        val height = etHeight.text?.toString()?.trim() ?: ""
        val weight = etWeight.text?.toString()?.trim() ?: ""
        
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnCreateAccount.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Create Firebase Auth user
                val authResult = FirebaseAuthService.signUp(email, password)
                
                if (authResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignupActivity, 
                            "Signup failed: ${authResult.exceptionOrNull()?.message}", 
                            Toast.LENGTH_LONG).show()
                        btnCreateAccount.isEnabled = true
                    }
                    return@launch
                }
                
                val firebaseUser = authResult.getOrNull()!!
                val userId = firebaseUser.uid
                
                // Create date of birth string
                val dateOfBirth = if (dobDay.isNotEmpty() && dobMonth.isNotEmpty() && dobYear.isNotEmpty()) {
                    "$dobDay/$dobMonth/$dobYear"
                } else {
                    ""
                }
                
                // Create user object
                val user = User(
                    id = userId,
                    name = name,
                    email = email,
                    age = age,
                    dateOfBirth = dateOfBirth,
                    height = height,
                    weight = weight
                )
                
                // Save user to Firestore
                val firestoreResult = FirestoreService.createUser(userId, user)
                
                if (firestoreResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignupActivity,
                            "Failed to create user profile: ${firestoreResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG).show()
                        btnCreateAccount.isEnabled = true
                    }
                    return@launch
                }
                
                // Save login state
                preferenceManager.setLoggedIn(true)
                preferenceManager.setUserId(userId)
                preferenceManager.setUserEmail(email)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                    finish()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    btnCreateAccount.isEnabled = true
                }
            }
        }
    }
}

