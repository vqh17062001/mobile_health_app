package com.example.mobile_health_app.ui.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import androidx.navigation.fragment.findNavController
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.databinding.FragmentChangePasswordBinding
import com.example.mobile_health_app.viewmodel.AuditLogViewModel
import com.example.mobile_health_app.viewmodel.UserViewModel
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mongodb.kbson.ObjectId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userViewModel: UserViewModel
    private lateinit var auditLogViewModel: AuditLogViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        auditLogViewModel = ViewModelProvider(requireActivity())[AuditLogViewModel::class.java]
        
        setupButtons()
        setupPasswordStrengthChecker()
        observeViewModel()
    }
    
    /**
     * Sets up password strength checker that provides real-time feedback
     */
    private fun setupPasswordStrengthChecker() {
        binding.edtNewPassword.addTextChangedListener(object : TextWatcher {
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
                    binding.tilNewPassword.helperText = feedbackMessage
                    binding.tilNewPassword.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
                    
                    // Display error only for weak passwords
                    if (passwordStrength.score < 2) {
                        binding.edtNewPassword.error = "Mật khẩu quá yếu"
                    } else {
                        binding.edtNewPassword.error = null
                    }
                } else {
                    // Hide strength info when password field is empty
                    binding.tilNewPassword.helperText = null
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
            0 -> feedback.append("Mật khẩu rất yếu! ")
            1 -> feedback.append("Mật khẩu yếu! ")
            2 -> feedback.append("Mật khẩu trung bình. ")
            3 -> feedback.append("Mật khẩu mạnh. ")
            4 -> feedback.append("Mật khẩu rất mạnh! ")
        }
        
        // Add specific feedback from the library
        val zxcvbnFeedback = strength.feedback
        if (!zxcvbnFeedback.warning.isNullOrEmpty()) {
            feedback.append(zxcvbnFeedback.warning)
        }
        
        if (zxcvbnFeedback.suggestions.isNotEmpty()) {
            feedback.append(" Gợi ý: ")
            feedback.append(zxcvbnFeedback.suggestions.joinToString(". "))
        }
        
        return feedback.toString()
    }
    
    private fun setupButtons() {
        // Submit button click
        binding.btnSubmitPassword.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }
        
        // Cancel button click - try multiple navigation approaches for reliability
        binding.btnCancel.setOnClickListener {
            try {
                // Option 1: Navigate up to previous destination
                if (!findNavController().popBackStack()) {
                    // Option 2: Navigate directly to account fragment if popBackStack fails
                    findNavController().navigate(R.id.navigation_account)
                }
            } catch (e: Exception) {
                // Option 3: Fallback to activity-level navigation if all else fails
                requireActivity().onBackPressed()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
        
        val currentPassword = binding.edtCurrentPassword.text.toString()
        val newPassword = binding.edtNewPassword.text.toString()
        val confirmPassword = binding.edtConfirmPassword.text.toString()
        
        // Validate current password
        if (currentPassword.isEmpty()) {
            binding.tilCurrentPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            isValid = false
        }
        
        // Validate new password with strength check
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
            binding.tilNewPassword.helperText = null
            isValid = false
        } else {
            // Use Zxcvbn to check password strength
            val strength = evaluatePasswordStrength(newPassword)
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
                binding.edtNewPassword.error = "Mật khẩu quá yếu"
                binding.tilNewPassword.helperText = feedback
                binding.tilNewPassword.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
                isValid = false
            } else {
                // For strong passwords, make sure there's no error
                binding.edtNewPassword.error = null
                binding.tilNewPassword.helperText = feedback
                binding.tilNewPassword.setHelperTextColor(android.content.res.ColorStateList.valueOf(textColor))
            }
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Vui lòng xác nhận mật khẩu mới"
            isValid = false
        } else if (confirmPassword != newPassword) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            isValid = false
        }
        
        return isValid
    }
    
    private fun changePassword() {
        val currentPassword = binding.edtCurrentPassword.text.toString()
        val newPassword = binding.edtNewPassword.text.toString()
        
        // Get user ID from activity or preferences
        val userIdString = activity?.intent?.getStringExtra("userId")
        
        if (userIdString != null) {
            try {
                val userId = ObjectId(userIdString)
                binding.progressBarPassword.visibility = View.VISIBLE
                
                // Call ViewModel to change password
                userViewModel.changePassword(userId, currentPassword, newPassword)
                
                // Log the event
                viewLifecycleOwner.lifecycleScope.launch {
                    val ipAddress = getPublicIpAddress() ?: "unknown"
                    val auditLog = AuditLog(
                        userId = userId,
                        eventAt = getCurrentTimeISO(),
                        action = "change-password",
                        resource = "users",
                        resourceId = userId,
                        ipAddress = ipAddress, // You can get IP address as you did in AccountFragment
                        detail = mapOf("updatedField" to "passwordHash")
                    )
                    auditLogViewModel.insertLog(auditLog)
                }
            } catch (e: Exception) {
                binding.progressBarPassword.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBarPassword.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSubmitPassword.isEnabled = !isLoading
                binding.btnCancel.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.passwordChangeSuccessful.collectLatest { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    userViewModel.resetPasswordChangeStatus()
                    findNavController().navigateUp()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    userViewModel.clearError()
                }
            }
        }
    }
    
    private fun getCurrentTimeISO(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO){
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()
            val response = client.newCall(request).execute()


            return@withContext response.body.string()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext e.message
        }

    }

}
