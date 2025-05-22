package com.example.mobile_health_app.ui
import com.example.mobile_health_app.ui.SignupActivity as SignupActivity
import com.example.mobile_health_app.ui.StartActivity as StartActivity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_health_app.MainActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.LoginBinding

class LoginActivity : AppCompatActivity() {    private lateinit var binding: LoginBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle Login button click
        binding.buttonLogin.setOnClickListener {
            // Navigate to MainActivity after successful login
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the login activity
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
            android.widget.Toast.makeText(this, "Forgot Password clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
