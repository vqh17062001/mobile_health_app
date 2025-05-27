package com.example.mobile_health_app.ui
import com.example.mobile_health_app.ui.SignupActivity as SignupActivity
import com.example.mobile_health_app.ui.StartActivity as StartActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.MainActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.viewmodel.UserViewModel
import com.example.mobile_health_app.databinding.LoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.Toast

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var userViewModel: UserViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        
        // Setup observers for loading and error states
        lifecycleScope.launch {
            userViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            userViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show()
                    userViewModel.clearError()
                }
            }
        }
        
        // Observe current user state
        lifecycleScope.launch {
            userViewModel.currentUser.collectLatest { user ->
                if (user != null) {
                    // Navigate to MainActivity after successful login
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close the login activity
                }
            }
        }
        
        // Handle Login button click
        binding.buttonLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPass.text.toString()
            
            if (username.isNotEmpty() && password.isNotEmpty()) {
                userViewModel.login(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle Sign Up text click
        binding.txtSignUp.setOnClickListener {
            // Navigate to SignupActivity
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            // Not calling finish() here so user can navigate back to login
        }
        
        // Handle Forgot Password click
        binding.txtForgotPassword.setOnClickListener {
            // Handle forgot password action
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
