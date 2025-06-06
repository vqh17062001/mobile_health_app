package com.example.mobile_health_app.ui.features

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.adapter.FeaturesAdapter
import com.example.mobile_health_app.data.model.Feature
import com.example.mobile_health_app.viewmodel.UserViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

class FeaturesFragment : Fragment() {
    
    private lateinit var userViewModel: UserViewModel
    private lateinit var featuresAdapter: FeaturesAdapter
    private lateinit var featuresList: List<Feature>
    private lateinit var gridView: GridView
    
    // Feature IDs
    private val FEATURE_ADD_MANAGER = "add_manager"
    private val TAG = "FeaturesFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Creating fragment")
        val root = inflater.inflate(R.layout.fragment_features, container, false)
        
        // Initialize ViewModel
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        
        // Set up the features grid
        gridView = root.findViewById(R.id.gridViewFeatures)
        setupFeatures()
        
        // Observe ViewModel
        observeViewModel()
        
        return root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: View created")
        
        // Additional click handling at the GridView level as backup
        setupGridViewClickListener()
    }

    private fun setupGridViewClickListener() {
        Log.d(TAG, "setupGridViewClickListener: Setting up click listeners")
        
        // Try setting the click listener again after the view is created
        gridView.setOnItemClickListener { parent, view, position, id ->
            Log.d(TAG, "GridView onItemClick: Item clicked at position $position")
            if (position < featuresList.size) {
                // This will show visual feedback that something happened
                view.isPressed = true
                
                Toast.makeText(context, getString(R.string.feature_selected, featuresList[position].title), Toast.LENGTH_SHORT).show()
                handleFeatureClick(featuresList[position])
            }
        }
    }
    
    private fun setupFeatures() {
        Log.d(TAG, "setupFeatures: Initializing features list")
        // Create features list
        featuresList = listOf(
            Feature(
                id = FEATURE_ADD_MANAGER,
                title = getString(R.string.feature_add_manager_title),
                description = getString(R.string.feature_add_manager_description),
                iconResourceId = R.drawable.icons8_add_administrator,
                action = "add_manager"
            ),
            // Add another feature for testing
            Feature(
                id = "view_health",
                title = getString(R.string.feature_view_health_title),
                description = getString(R.string.feature_view_health_description),
                iconResourceId = android.R.drawable.ic_menu_view,
                action = "view_health"
            )
        )
        
        // Set up adapter with click listener
        featuresAdapter = FeaturesAdapter(
            requireContext(), 
            featuresList,
            onItemClickListener = { feature ->
                Log.d(TAG, "Adapter item clicked: ${feature.id}")
                Toast.makeText(requireContext(), getString(R.string.feature_selected, feature.title), Toast.LENGTH_SHORT).show()
                handleFeatureClick(feature)
            }
        )
        
        gridView.adapter = featuresAdapter
        
        // Additional settings to make GridView more responsive
        gridView.isClickable = true
        gridView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        
        Log.d(TAG, "setupFeatures: Features list size: ${featuresList.size}")
    }
    
    private fun handleFeatureClick(feature: Feature) {
        Log.d(TAG, "handleFeatureClick: Feature clicked: ${feature.id}")
        when(feature.id) {
            FEATURE_ADD_MANAGER -> {
                Log.d(TAG, "handleFeatureClick: Showing add manager dialog")
                showAddManagerDialog()
            }
            else -> {
                Toast.makeText(context, getString(R.string.feature_not_implemented, feature.title), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddManagerDialog() {
        Log.d(TAG, "showAddManagerDialog: Creating dialog")
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_manager, null)
        
        val inputManagerId = dialogView.findViewById<TextInputEditText>(R.id.editTextManagerId)
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_manager_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                val managerId = inputManagerId.text.toString().trim()
                if (managerId.isNotEmpty()) {
                    // Validate the manager ID
                    if (isValidObjectId(managerId)) {
                        updateManagerId(managerId)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.invalid_manager_id), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.manager_id_empty), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    
    private fun isValidObjectId(id: String): Boolean {
        return try {
           id
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun updateManagerId(managerId: String) {
        // Get current user ID from activity
        val userIdString = activity?.intent?.getStringExtra("userId")
        
        if (userIdString != null) {
            try {
                val userId = ObjectId(userIdString)
                
                // Get current user data
                val currentUser = userViewModel.currentUser.value
                
                // Update the managerIds field
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        managerIds = managerId,
                        updatedAt = getCurrentTimeISO()
                    )
                    
                    // Update user in database
                    userViewModel.updateUserManagerIds(updatedUser)
                } else {
                    // If user data not loaded yet, load it first
                    userViewModel.loadUserById(userId)
                    Toast.makeText(requireContext(), getString(R.string.try_after_user_data_loaded), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_occurred, e.message), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.user_id_not_found), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getCurrentTimeISO(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    userViewModel.clearError()
                }
            }
        }
    }
}
