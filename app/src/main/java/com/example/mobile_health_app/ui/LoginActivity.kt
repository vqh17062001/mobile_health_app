package com.example.mobile_health_app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.ui.MainActivity
import com.example.mobile_health_app.R
import com.example.mobile_health_app.viewmodel.UserViewModel
import com.example.mobile_health_app.databinding.LoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.data.model.Device
import com.example.mobile_health_app.viewmodel.AuditLogViewModel
import com.example.mobile_health_app.viewmodel.DeviceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mongodb.kbson.ObjectId

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var auditLogViewModel: AuditLogViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the NoActionBar theme for full screen experience
        setTheme(R.style.Theme_Mobile_health_app_NoActionBar)
        
        // Set the status bar to light color with dark icons
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        deviceViewModel = ViewModelProvider(this)[DeviceViewModel::class.java]
        auditLogViewModel = ViewModelProvider(this)[AuditLogViewModel::class.java]

        // Setup observers for loading and error states
        lifecycleScope.launch {
            userViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            userViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show()
                    userViewModel.clearError()
                }
            }
        }
        
        // Observe current user state
        lifecycleScope.launch {
            userViewModel.currentUser.collectLatest { user ->
                if (user != null) {
                    // 1. Lấy deviceId và thông tin thiết bị
                    val deviceId = getCurrentDeviceId()
                    val device = Device(
                        deviceId = deviceId,
                        ownerId = user._id,
                        model = android.os.Build.MODEL ?: "",
                        osVersion = android.os.Build.VERSION.RELEASE ?: "",
                        sdkVersion = android.os.Build.VERSION.SDK_INT.toString(),
                        registeredAt = getCurrentTimeISO(),
                        lastSyncAt = getCurrentTimeISO(),
                        status = "online"
                    )

                    // 2. Gọi tới DeviceViewModel để kiểm tra/thêm/cập nhật thiết bị
                    deviceViewModel.fetchDeviceById(deviceId, user._id)

                    val existedDevice = withTimeoutOrNull(4000) {
                        deviceViewModel.currentDevice
                            .filterNotNull()
                            .first()
                    }

                    // 3. Xử lý tiếp
                    if (existedDevice == null) {
                        deviceViewModel.insertDevice(device)
                    } else {
                        deviceViewModel.updateDeviceStatus(deviceId, "online")
                    }


                    /// 4. Ghi log đăng nhập
                    val ipAddress = getPublicIpAddress() ?: "unknown"
                    val auditLog = AuditLog(
                        userId = user._id,
                        eventAt = getCurrentTimeISO(),
                        action = "login",
                        resource = "users",
                        resourceId = user._id,
                        ipAddress = ipAddress, // Bạn có thể lấy IP nếu cần
                        detail = mapOf("deviceId" to deviceId, "deviceModel" to device.model)
                    )
                    auditLogViewModel.insertLog(auditLog)

                    // 5. Chuyển sang MainActivity sau khi xong (bạn có thể show loading cho đẹp)
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)

                    intent.putExtra("deviceId", deviceId)
                    intent.putExtra("userId", user._id.toString())
                    intent.putExtra("userFullName", bsonStringToValue(user.fullName.toString()) ?: "")
                    intent.putExtra("userEmail",bsonStringToValue(user.email.toString()) ?: "")
                    intent.putExtra("userName", bsonStringToValue(user.username.toString() )?: "")

                    startActivity(intent)
                    finish()

                }else {
                    /// 4. Ghi log đăng nhập thất bại
                    val ipAddress = getPublicIpAddress() ?: "unknown"
                    val auditLog = AuditLog(

                        eventAt = getCurrentTimeISO(),
                        action = "login-failed",
                        resource = "users",

                        ipAddress = ipAddress,
                        detail = mapOf("reason" to "login failed")
                    )

                    auditLogViewModel.insertLog(auditLog)
                }
            }
        }


        // Handle Login button click
        binding.buttonLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPass.text.toString()
            
            if (username.isNotEmpty() && password.isNotEmpty()) {
                userViewModel.loginUser(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle Sign Up text click
        binding.txtSignUp.setOnClickListener {
            // Navigate to SignupActivity
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            // Not calling finish() here so user can navigate back to login
        }
        
        // Handle Forgot Password click
        binding.txtForgotPassword.setOnClickListener {
            // Handle forgot password action
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getCurrentDeviceId(): String {
        // Android ID - ổn định cho mỗi thiết bị + app install
        return android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }


    private fun getCurrentTimeISO(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    fun bsonStringToValue(s: String): String {
        return if (s.startsWith("BsonString(value='")) {
            s.removePrefix("BsonString(value='").removeSuffix("')")
        } else s
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
