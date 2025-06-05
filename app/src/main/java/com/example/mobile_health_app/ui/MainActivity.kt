package com.example.mobile_health_app.ui

import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.ActivityMainBinding
import com.example.mobile_health_app.ui.account.AccountFragment
import com.example.mobile_health_app.ui.features.FeaturesFragment
import com.example.mobile_health_app.ui.qrscan.QrScanFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure we're using system dark mode settings
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user data from intent
        val username = intent.getStringExtra("userName")
        val userId = intent.getStringExtra("userId")
        val userEmail = intent.getStringExtra("userEmail")
        val userFullName = intent.getStringExtra("userFullName")

        // Set up bottom navigation
        setupBottomNavigation()

        // Set default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, FeaturesFragment())
                .commit()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = binding.bottomNavigation
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_features -> FeaturesFragment()
                R.id.navigation_qr_scan -> QrScanFragment()
                R.id.navigation_account -> AccountFragment()
                else -> FeaturesFragment()
            }
            replaceFragment(fragment)
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, fragment)
            .commit()
    }

    // For external access from other fragments
    fun loadFragment(fragment: Fragment) {
        replaceFragment(fragment)
    }
}