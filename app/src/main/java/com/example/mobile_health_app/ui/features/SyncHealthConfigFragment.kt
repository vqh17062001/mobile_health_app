package com.example.mobile_health_app.ui.features

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.FragmentSyncHealthConfigBinding

class SyncHealthConfigFragment : Fragment() {

    private var _binding: FragmentSyncHealthConfigBinding? = null
    private val binding get() = _binding!!
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncHealthConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy user ID từ arguments hoặc activity
        currentUserId = arguments?.getString("userId") ?: activity?.intent?.getStringExtra("userId")
        
        setupButtons()
        loadSavedConfiguration()
    }    private fun setupButtons() {
        // Close button


        // Cancel button
        binding.btnCancel.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Save configuration button
        binding.btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }
    }private fun loadSavedConfiguration() {
        currentUserId?.let { userId ->
            val sharedPrefs = requireContext().getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
            
            // Load config cho user hiện tại
            binding.switchActivity.isChecked = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userId), false)
            binding.switchSleep.isChecked = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SLEEP, userId), false)
            binding.switchHeartRate.isChecked = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userId), false)
            binding.switchSpO2.isChecked = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SPO2, userId), false)
        }
    }

    private fun saveConfiguration() {
        currentUserId?.let { userId ->
            val sharedPrefs = requireContext().getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
            
            with(sharedPrefs.edit()) {
                // Lưu user ID hiện tại
                putString(KEY_USER_ID, userId)
                
                // Lưu config theo user ID
                putBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userId), binding.switchActivity.isChecked)
                putBoolean(getKeyForUser(KEY_SYNC_SLEEP, userId), binding.switchSleep.isChecked)
                putBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userId), binding.switchHeartRate.isChecked)
                putBoolean(getKeyForUser(KEY_SYNC_SPO2, userId), binding.switchSpO2.isChecked)
                apply()
            }

            // Show confirmation message
            val enabledTypes = mutableListOf<String>()
            if (binding.switchActivity.isChecked) enabledTypes.add(getString(R.string.activity_data))
            if (binding.switchSleep.isChecked) enabledTypes.add(getString(R.string.sleep_data))
            if (binding.switchHeartRate.isChecked) enabledTypes.add(getString(R.string.heart_rate_data))
            if (binding.switchSpO2.isChecked) enabledTypes.add(getString(R.string.spo2_data))

            val message = if (enabledTypes.isNotEmpty()) {
                getString(R.string.sync_config_saved_with_types, enabledTypes.joinToString(", "))
            } else {
                getString(R.string.sync_config_saved_no_types)
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getKeyForUser(baseKey: String, userId: String): String {
        return "${baseKey}_$userId"
    }

    companion object {
        const val TAG = "SyncHealthConfigFragment"
        private const val SYNC_PREFS_NAME = "health_sync_config"
        private const val KEY_USER_ID = "user_id"
        const val KEY_SYNC_ACTIVITY = "sync_activity"
        const val KEY_SYNC_SLEEP = "sync_sleep"
        const val KEY_SYNC_HEART_RATE = "sync_heart_rate"
        const val KEY_SYNC_SPO2 = "sync_spo2"

        @JvmStatic
        fun newInstance(userId: String? = null) = SyncHealthConfigFragment().apply {
            arguments = Bundle().apply {
                userId?.let { putString("userId", it) }
            }
        }
    }
}