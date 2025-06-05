package com.example.mobile_health_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.ActivityLanguageBinding
import com.example.mobile_health_app.util.LocaleHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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
        
        // Replace RadioGroup with ChipGroup in your layout XML
        val chipGroup = binding.chipGroupLanguages  // Assume you've changed this in your layout
        
        // Clear existing chips if any
        chipGroup.removeAllViews()
        
        // Set single selection mode
        chipGroup.isSingleSelection = true
        chipGroup.isSelectionRequired = true
        
        // Add a chip for each language
        languages.forEach { lang ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                tag = lang.code
                text = lang.displayName
                isCheckable = true
                
                // Set flag icon
                val flagResourceId = when(lang.code) {
                    "en" -> R.drawable.icons8_usa
                    "vi" -> R.drawable.icons8_vietnam
                    "ru" -> R.drawable.icons8_russian
                    else -> R.drawable.icons8_usa
                }
                setChipIconResource(flagResourceId)
                
                // Style the chip - use primary color when selected
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                chipStrokeWidth = 1f
                chipStrokeColor = resources.getColorStateList(R.color.chip_stroke_selector, null)
                
                // Check if this is the currently selected language
                isChecked = (lang.code == selectedLanguageCode)
            }
            
            // Add to chip group
            chipGroup.addView(chip)
        }
        
        // Add listener for selection changes
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = findViewById<Chip>(checkedIds[0])
                selectedLanguageCode = selectedChip.tag as String
            }
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
