package com.mealsai.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mealsai.app.services.FirebaseAuthService
import com.mealsai.app.ui.generate.GenerateMealFragment
import com.mealsai.app.utils.PreferenceManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private val preferenceManager by lazy { PreferenceManager.getInstance(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        if (!preferenceManager.isLoggedIn() || !FirebaseAuthService.isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        
        try {
            bottomNavigation = findViewById(R.id.bottomNavigation)
            
            // Setup logout button in header (if exists in fragments)
            setupLogoutButton()
            
            // Load default fragment
            if (savedInstanceState == null) {
                loadFragment(GenerateMealFragment())
            }
            
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_generate -> {
                        loadFragment(GenerateMealFragment())
                        true
                    }
                    R.id.nav_plan -> {
                        loadFragment(WeeklyPlanFragment())
                        true
                    }
                    R.id.nav_saved -> {
                        loadFragment(SavedMealsFragment())
                        true
                    }
                    R.id.nav_grocery -> {
                        loadFragment(GroceryListFragment())
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupLogoutButton() {
        // This will be called from fragments that have the header
        // For now, we'll handle logout via a long-press on the app name or add it to a menu
    }
    
    fun handleLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuthService.signOut()
                preferenceManager.clear()
                preferenceManager.setLoggedIn(false)
                
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        
        // Setup logout button after fragment is loaded
        setupLogoutInFragment()
    }
    
    private fun setupLogoutInFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        fragment?.view?.let { view ->
            val headerView = view.findViewById<View>(R.id.appHeader)
            headerView?.findViewById<MaterialButton>(R.id.btnLogout)?.setOnClickListener {
                handleLogout()
            }
        }
    }
}
