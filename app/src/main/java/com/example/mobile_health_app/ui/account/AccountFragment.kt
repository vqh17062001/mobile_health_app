package com.example.mobile_health_app.ui.account

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.databinding.FragmentAccountBinding
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var auditLogViewModel: AuditLogViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        auditLogViewModel = ViewModelProvider(requireActivity())[AuditLogViewModel::class.java]

        
        setupGenderDropdown()
        setupDatePicker()
        setupButtons()
        loadUserData()
        observeUserViewModel()
    }
    
    private fun setupGenderDropdown() {
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        
        // Set adapter for AutoCompleteTextView
        (binding.edtGender as AutoCompleteTextView).setAdapter(adapter)
        
        // Show dropdown when field is clicked
        binding.edtGender.setOnClickListener {
            (binding.edtGender as AutoCompleteTextView).showDropDown()
        }
    }
    
    private fun setupDatePicker() {
        binding.edtBirthday.setOnClickListener {
            // Get current date
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            // Create DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format the date and set it in the field
                    val selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
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
    
    private fun setupButtons() {
        // Save button click
        binding.btnSaveProfile.setOnClickListener {
            // Validate inputs
            if (validateInputs()) {

                saveUserData()
            }
        }
        
        // Change password button click
        binding.btnChangePassword.setOnClickListener {
            // Navigate to change password screen or show dialog
            showChangePasswordDialog()
        }
    }
    
    private fun loadUserData() {
        // Get user ID from activity or preferences
        val userIdString = activity?.intent?.getStringExtra("userId")
        
        if (userIdString != null) {
            try {
                val userId = ObjectId(userIdString)
                
                // Set loading state
                binding.progressBar.visibility = View.VISIBLE
                
                // Load user data
                userViewModel.loadUserById(userId)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Không thể tải thông tin người dùng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeUserViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.currentUser.collectLatest { user ->
                if (user != null) {
                    // Format display values
                    binding.edtUsername.setText(bsonStringToValue(user.username))
                    binding.edtFullName.setText(bsonStringToValue(user.fullName))
                    binding.edtEmail.setText(bsonStringToValue(user.email))
                    binding.edtPhone.setText(bsonStringToValue(user.phone))
                    binding.edtGender.setText(bsonStringToValue(user.gender))
                    binding.edtDepartment.setText(bsonStringToValue(user.department))
                    binding.edtRole.setText(bsonStringToValue(user.role))

                    
                    // Format date from ISO to display format (DD/MM/YYYY)
                    val dob = bsonStringToValue(user.Dob)
                    binding.edtBirthday.setText(formatDisplayDate(dob))
                    
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSaveProfile.isEnabled = !isLoading
                binding.btnChangePassword.isEnabled = !isLoading
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
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        with(binding) {
            // Check full name
            if (edtFullName.text.toString().trim().isEmpty()) {
                edtFullName.error = "Vui lòng nhập họ tên"
                isValid = false
            }
            
            // Check email
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                edtEmail.error = "Vui lòng nhập email"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Email không hợp lệ"
                isValid = false
            }
            
            // Check phone
            if (edtPhone.text.toString().trim().isEmpty()) {
                edtPhone.error = "Vui lòng nhập số điện thoại"
                isValid = false
            }
            
            // Check gender
            if (edtGender.text.toString().trim().isEmpty()) {
                edtGender.error = "Vui lòng chọn giới tính"
                isValid = false
            }
            
            // Check birthday
            if (edtBirthday.text.toString().trim().isEmpty()) {
                edtBirthday.error = "Vui lòng chọn ngày sinh"
                isValid = false
            }
        }
        
        return isValid
    }
    
    private fun saveUserData() {
        val userIdString = activity?.intent?.getStringExtra("userId")
        
        if (userIdString != null) {
            try {
                val userId = ObjectId(userIdString)
                
                val updatedUser = userViewModel.currentUser.value?.copy(
                    fullName = binding.edtFullName.text.toString().trim(),
                    email = binding.edtEmail.text.toString().trim(),
                    phone = binding.edtPhone.text.toString().trim(),
                    gender = binding.edtGender.text.toString().trim(),
                    Dob = formatDateForDb(binding.edtBirthday.text.toString().trim()),
                    department = binding.edtDepartment.text.toString().trim(),
                    updatedAt = getCurrentTimeISO()
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    val ipAddress = getPublicIpAddress() ?: "unknown"
                    val auditLog = AuditLog(
                        userId = updatedUser?._id,
                        eventAt = getCurrentTimeISO(),
                        action = "update-profile",
                        resource = "users",
                        resourceId = updatedUser?._id,
                        ipAddress = ipAddress,
                        detail = mapOf(
                            "updatedFields" to listOf(
                                "fullName", "email", "phone",
                                "gender", "Dob", "department"
                            ).joinToString(",")
                        )
                    )
                    auditLogViewModel.insertLog(auditLog)
                }
                
                if (updatedUser != null) {
                    userViewModel.updateUserProfile(updatedUser)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Cập nhật thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showChangePasswordDialog() {
        // Create a dialog to change password
        Toast.makeText(requireContext(), "Tính năng đổi mật khẩu sẽ được cập nhật sau", Toast.LENGTH_SHORT).show()
        
        // You would implement a dialog here with current password, new password and confirm password fields
    }
    
    // Format date from UI format (DD/MM/YYYY) to ISO format (YYYY-MM-DDT00:00:00Z)
    private fun formatDateForDb(uiDate: String): String {
        try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'", Locale.getDefault())
            val date = inputFormat.parse(uiDate)
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            return uiDate // Return original if parsing fails
        }
    }
    
    // Format date from ISO format to UI format (DD/MM/YYYY)
    private fun formatDisplayDate(isoDate: String): String {
        try {
            if (isoDate.contains("T")) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(isoDate.replace("Z", ""))
                return outputFormat.format(date ?: Date())
            }
            return isoDate
        } catch (e: Exception) {
            return isoDate // Return original if parsing fails
        }
    }
    
    private fun getCurrentTimeISO(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    
    // Helper function to extract value from BsonString representation
    private fun bsonStringToValue(s: String): String {
        return if (s.startsWith("BsonString(value='")) {
            s.removePrefix("BsonString(value='").removeSuffix("')")
        } else s
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
