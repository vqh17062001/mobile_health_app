package com.example.mobile_health_app.ui.account

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.databinding.FragmentAccountBinding
import com.example.mobile_health_app.ui.LoginActivity
import com.example.mobile_health_app.viewmodel.AuditLogViewModel
import com.example.mobile_health_app.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mongodb.kbson.ObjectId
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var auditLogViewModel: AuditLogViewModel
    private var currentPhotoPath: String? = null
    private var userId: String? = null

    // Register activity result for gallery image selection
    private val selectImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Save and display the selected image
                saveAndDisplaySelectedImage(uri)
            }
        }
    }
    
    // Register activity result for camera image capture
    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                // Save and display the captured image
                val bitmap = BitmapFactory.decodeFile(path)
                saveImageToInternalStorage(bitmap)
                binding.profileImage.setImageBitmap(bitmap)
            }
        }
    }
    
    // Permission request for camera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to take pictures",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
        
        userId = activity?.intent?.getStringExtra("userId")
        
        setupGenderDropdown()
        setupDatePicker()
        setupButtons()
        loadUserData()
        observeUserViewModel()
        
        // Load profile image if exists
        loadProfileImage()
        
        // Setup profile image click
        binding.profileImage.setOnClickListener {
            checkCameraPermissionAndProceed()
        }
        
        binding.cameraIcon.setOnClickListener {
            checkCameraPermissionAndProceed()
        }
        
        // Setup logout click
        binding.txtLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }
    
    private fun loadProfileImage() {
        try {
            userId?.let { id ->
                val imageFile = File(requireContext().filesDir, "profile_$id.jpg")
                if (imageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    binding.profileImage.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun checkCameraPermissionAndProceed() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                showImageSourceDialog()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show explanation why camera permission is needed
                AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("This app needs camera access to take profile pictures")
                    .setPositiveButton("Allow") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhotoWithCamera()
                    1 -> pickImageFromGallery()
                    2 -> { /* cancel */ }
                }
            }
            .show()
    }
    
    private fun takePhotoWithCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.mobile_health_app.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePicture.launch(takePictureIntent)
                }
            }
        }
    }
    
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageFromGallery.launch(intent)
    }
    
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
    
    private fun saveAndDisplaySelectedImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        saveImageToInternalStorage(bitmap)
        binding.profileImage.setImageBitmap(bitmap)
    }
    
    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            userId?.let { id ->
                val file = File(requireContext().filesDir, "profile_$id.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Failed to save profile image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }
    
    private fun performLogout() {
        // Log the logout event
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                userId?.let { id ->
                    val objectId = ObjectId(id)
                    val ipAddress = getPublicIpAddress() ?: "unknown"
                    val auditLog = AuditLog(
                        userId = objectId,
                        eventAt = getCurrentTimeISO(),
                        action = "logout",
                        resource = "users",
                        resourceId = objectId,
                        ipAddress = ipAddress,
                        detail = mapOf("method" to "user-initiated")
                    )
                    auditLogViewModel.insertLog(auditLog)
                }

                // Clear any saved credentials or tokens
                val preferences = requireActivity().getSharedPreferences(
                    "app_preferences",
                    Context.MODE_PRIVATE
                )
                preferences.edit().remove("user_id").apply()

                // Return to login screen
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Logout failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
        // Navigate to ChangePasswordFragment using Navigation Components
        // Replace with direct fragment transaction since the navigation ID doesn't exist
        val changePasswordFragment = ChangePasswordFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, changePasswordFragment)
            .addToBackStack(null)
            .commit()
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
