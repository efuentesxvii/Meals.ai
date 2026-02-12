package com.mealsai.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mealsai.app.services.FirebaseAuthService
import com.mealsai.app.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnCreateAccount: MaterialButton
    
    private val preferenceManager by lazy { PreferenceManager.getInstance(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        if (preferenceManager.isLoggedIn() && FirebaseAuthService.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            handleLogin()
        }
        
        btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
    
    private lateinit var progressBar: android.widget.ProgressBar

    private fun showLoading(show: Boolean) {
        if (show) {
            btnLogin.text = ""
            btnLogin.icon = ContextCompat.getDrawable(this, R.drawable.loading_anim)
            btnLogin.isEnabled = false
        } else {
            btnLogin.text = "Log In"
            btnLogin.icon = null
            btnLogin.isEnabled = true
        }
    }
    
    private fun handleLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = FirebaseAuthService.signIn(email, password)
                
                if (result.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity,
                            "Login failed: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG).show()
                        showLoading(false)
                    }
                    return@launch
                }
                
                val user = result.getOrNull()!!
                
                // Save login state
                preferenceManager.setLoggedIn(true)
                preferenceManager.setUserId(user.uid)
                preferenceManager.setUserEmail(user.email ?: "")
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
        }
    }
}
