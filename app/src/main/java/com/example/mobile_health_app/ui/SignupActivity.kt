package com.example.mobile_health_app.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.viewmodel.UserViewModel
import com.example.mobile_health_app.databinding.SignupBinding
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: SignupBinding
    private lateinit var userViewModel: UserViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding = SignupBinding.inflate(layoutInflater)
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
                    Toast.makeText(this@SignupActivity, it, Toast.LENGTH_SHORT).show()
                    userViewModel.clearError()
                }
            }
        }
        
        // Setup gender dropdown
        setupGenderDropdown()
        
        // Setup date picker for birthday field
        setupDatePicker()
        
        // Handle Sign Up button click
        binding.buttonSignup.setOnClickListener {
            // Here you would validate the form fields
            if (validateForm()) {
                // Register the user using ViewModel
                userViewModel.register(
                    username = binding.edtUsername.text.toString().trim(),
                    password = binding.edtPassword.text.toString(),
                    fullName = binding.edtFullName.text.toString().trim(),
                    gender = binding.edtGender.text.toString().trim(),
                    dob = binding.edtBirthday.text.toString().trim(),
                    email = binding.edtEmail.text.toString().trim(),
                    phone = binding.edtPhone.text.toString().trim()
                )
                
                // Show success message
                Toast.makeText(this, "Sign Up successful!", Toast.LENGTH_SHORT).show()
                
                // Navigate to Login screen after successful registration
                val intent = Intent(this, com.example.mobile_health_app.ui.LoginActivity::class.java)
                startActivity(intent)
                finish() // Close the signup activity
            }
        }
        
        // Handle Login text click
        binding.txtLogin.setOnClickListener {
            // Navigate to Login screen
            val intent = Intent(this, com.example.mobile_health_app.ui.LoginActivity::class.java)
            startActivity(intent)
            finish() // Close the signup activity
        }
        
        // Add password strength checker as TextWatcher
        setupPasswordStrengthChecker()
    }
    
    /**
     * Sets up password strength checker that provides real-time feedback
     */
    private fun setupPasswordStrengthChecker() {
        binding.edtPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                
                if (password.isNotEmpty()) {
                    val passwordStrength = evaluatePasswordStrength(password)
                    val feedbackMessage = getPasswordFeedback(passwordStrength)
                    
                    // Set text color based on password strength
                    val textColor = when (passwordStrength.score) {
                        0 -> android.graphics.Color.parseColor("#FF0000") // Red for very weak
                        1 -> android.graphics.Color.parseColor("#FF8C00") // Orange for weak
                        2 -> android.graphics.Color.parseColor("#FFC000") // Amber for medium
                        3 -> android.graphics.Color.parseColor("#32CD32") // Green for strong
                        4 -> android.graphics.Color.parseColor("#006400") // Dark green for very strong
                        else -> android.graphics.Color.parseColor("#7B6BA8") // Default purple
                    }
                    
                    // Set the helper text with appropriate color
                    binding.passwordLayout.helperText = feedbackMessage
                    binding.passwordLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
                    
                    // Display error only for weak passwords
                    if (passwordStrength.score < 2) {
                        binding.edtPassword.error = "Password too weak"
                    } else {
                        binding.edtPassword.error = null
                    }
                } else {
                    // Hide strength info when password field is empty
                    binding.passwordLayout.helperText = null
                }
            }
        })
    }
    
    /**
     * Evaluates password strength using the Zxcvbn library
     */
    private fun evaluatePasswordStrength(password: String): Strength {
        val zxcvbn = Zxcvbn()
        return zxcvbn.measure(password)
    }
    
    /**
     * Generates user-friendly feedback based on password strength
     */
    private fun getPasswordFeedback(strength: Strength): String {
        val feedback = StringBuilder()
        
        // Add score-based assessment
        when (strength.score) {
            0 -> feedback.append("Very weak password! ")
            1 -> feedback.append("Weak password! ")
            2 -> feedback.append("Medium strength password. ")
            3 -> feedback.append("Strong password. ")
            4 -> feedback.append("Very strong password! ")
        }
        
        // Add specific feedback from the library
        val zxcvbnFeedback = strength.feedback
        if (!zxcvbnFeedback.warning.isNullOrEmpty()) {
            feedback.append(zxcvbnFeedback.warning)
        }
        
        if (zxcvbnFeedback.suggestions.isNotEmpty()) {
            feedback.append(" Suggestions: ")
            feedback.append(zxcvbnFeedback.suggestions.joinToString(". "))
        }
        
        return feedback.toString()
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
            }            // Check password
            val password = edtPassword.text.toString()
            if (password.isEmpty()) {
                edtPassword.error = "Password required"
                passwordLayout.helperText = null
                isValid = false
            } else {
                // Use Zxcvbn to check password strength
                val strength = evaluatePasswordStrength(password)
                val feedback = getPasswordFeedback(strength)
                
                // Set text color based on password strength
                val textColor = when (strength.score) {
                    0 -> android.graphics.Color.parseColor("#FF0000") // Red for very weak
                    1 -> android.graphics.Color.parseColor("#FF8C00") // Orange for weak
                    else -> android.graphics.Color.parseColor("#32CD32") // Green for medium+ strength
                }
                
                // Require at least a medium-strength password (score >= 2)
                if (strength.score < 2) {
                    // Show error for weak passwords
                    edtPassword.error = "Password too weak"
                    passwordLayout.helperText = feedback
                    passwordLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
                    isValid = false
                } else {
                    // For strong passwords, make sure there's no error
                    edtPassword.error = null
                    passwordLayout.helperText = feedback
                    passwordLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
                }
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
