package com.example.mobile_health_app.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.SignupBinding
import java.util.Calendar

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: SignupBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
          binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup gender dropdown
        setupGenderDropdown()
        
        // Setup date picker for birthday field
        setupDatePicker()
        
        // Handle Sign Up button click
        binding.buttonSignup.setOnClickListener {
            // Here you would validate the form fields
            if (validateForm()) {
                // Navigate to MainActivity or LoginActivity after successful signup
                // For now just show a toast message
                Toast.makeText(this, "Sign Up successful!", Toast.LENGTH_SHORT).show()
                
                // Navigate to Login screen after successful registration
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Close the signup activity
            }
        }
        
        // Handle Login text click
        binding.txtLogin.setOnClickListener {
            // Navigate to Login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close the signup activity
        }
    }
      /**
     * Sets up the gender dropdown with options from string resources
     */
    private fun setupGenderDropdown() {
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        
        // Set adapter for AutoCompleteTextView
        (binding.edtGender as? android.widget.AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            // Show dropdown when field is clicked
            setOnClickListener {
                showDropDown()
            }
        }
    }
    
    /**
     * Sets up the date picker for birthday field
     */
    private fun setupDatePicker() {
        binding.edtBirthday.setOnClickListener {
            // Get current date
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            // Create DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format the date and set it in the field
                    val selectedDate = "%02d/%02d/%d".format(selectedDay, selectedMonth + 1, selectedYear)
                    binding.edtBirthday.setText(selectedDate)
                },
                year,
                month,
                day
            )
            
            // Set max date to current date (no future dates)
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
            
            // Show the dialog
            datePickerDialog.show()
        }
    }
    
    private fun validateForm(): Boolean {
        // Simple validation - check if fields are not empty
        var isValid = true
        
        with(binding) {
            // Check full name
            if (edtFullName.text.toString().trim().isEmpty()) {
                edtFullName.error = "Full name required"
                isValid = false
            }
            
            // Check gender
            if (edtGender.text.toString().trim().isEmpty()) {
                edtGender.error = "Gender required"
                isValid = false
            }
            
            // Check birthday
            if (edtBirthday.text.toString().trim().isEmpty()) {
                edtBirthday.error = "Birthday required"
                isValid = false
            }
            
            // Check phone
            if (edtPhone.text.toString().trim().isEmpty()) {
                edtPhone.error = "Phone required"
                isValid = false
            }
            
            // Check email
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                edtEmail.error = "Email required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Please enter a valid email address"
                isValid = false
            }
            
            // Check username
            if (edtUsername.text.toString().trim().isEmpty()) {
                edtUsername.error = "Username required"
                isValid = false
            }


            // Check password
            val password = edtPassword.text.toString()
            if (password.isEmpty()) {
                edtPassword.error = "Password required"
                isValid = false
            } else if (password.length < 6) {
                edtPassword.error = "Password must be at least 6 characters"
                isValid = false
            }
            
            // Check confirm password
            val confirmPassword = edtConfirmPassword.text.toString()
            if (confirmPassword.isEmpty()) {
                edtConfirmPassword.error = "Please confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                edtConfirmPassword.error = "Passwords do not match"
                isValid = false
            }
        }
        
        return isValid
    }
}
