package com.example.mobile_health_app.ui
import com.example.mobile_health_app.ui.StartActivity as StartActivity
import com.example.mobile_health_app.ui.SignupActivity as SignupActivity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.StartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: StartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding = StartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Sign Up button click
        binding.buttonSignup.setOnClickListener {
            // Navigate to SignupActivity to show signup screen
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            // Not calling finish() here so user can go back to start screen if needed
        }

        // Handle Login button click
        binding.buttonLogin.setOnClickListener {
            // Navigate to LoginActivity to show login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Not calling finish() here so user can go back to start screen if needed
        }
    }
}
