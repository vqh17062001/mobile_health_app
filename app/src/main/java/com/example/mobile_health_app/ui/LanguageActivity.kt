package com.example.mobile_health_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.ActivityLanguageBinding
import com.example.mobile_health_app.util.LocaleHelper

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguageCode: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set title
        title = getString(R.string.language_selection)
        
        // Get current language
        selectedLanguageCode = LocaleHelper.getLanguage(this)
        
        // Setup language options
        setupLanguageOptions()
        
        // Setup buttons
        binding.btnSave.setOnClickListener {
            saveLanguageSelection()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupLanguageOptions() {
        val languages = LocaleHelper.getAvailableLanguages()
        val radioGroup = binding.radioGroupLanguages
        
        // Clear existing radio buttons if any
        radioGroup.removeAllViews()
        
        // Add radio button for each language
        languages.forEach { lang ->
            val radioButton = RadioButton(this)
            radioButton.id = View.generateViewId()
            radioButton.text = lang.displayName
            radioButton.tag = lang.code
            
            // Check the current language
            if (lang.code == selectedLanguageCode) {
                radioButton.isChecked = true
            }
            
            radioGroup.addView(radioButton)
        }
        
        // Listen for language selection
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            selectedLanguageCode = selectedRadioButton.tag as String
        }
    }
    
    private fun saveLanguageSelection() {
        // Apply language change
        LocaleHelper.setLocale(this, selectedLanguageCode)
        
        Toast.makeText(this, getString(R.string.language_selection), Toast.LENGTH_SHORT).show()
        
        // Restart the app to apply changes
        val intent = Intent(this, StartActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
}
