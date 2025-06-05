package com.example.mobile_health_app.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.databinding.FragmentChangePasswordBinding
import com.example.mobile_health_app.viewmodel.AuditLogViewModel
import com.example.mobile_health_app.viewmodel.UserViewModel
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
        observeViewModel()
    }
    
    private fun setupButtons() {
        // Submit button click
        binding.btnSubmitPassword.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }
        
        // Cancel button click
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
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
        
        // Validate new password
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
            isValid = false
        } else if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            isValid = false
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
