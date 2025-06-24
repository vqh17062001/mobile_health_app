package com.example.mobile_health_app.ui.features

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.databinding.FragmentDeviceInfoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.NetworkInterface

class DeviceInfoFragment : Fragment() {

    private var _binding: FragmentDeviceInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var sensorManager: SensorManager
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)        // Initialize system services
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Initialize Bluetooth
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Load device information
        loadDeviceInformation()

        // Set refresh button click listener
        binding.btnRefreshInfo.setOnClickListener {
            loadDeviceInformation()
            Toast.makeText(requireContext(), "Device information refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDeviceInformation() {
        loadBasicDeviceInfo()
        loadNetworkInfo()
        loadHealthSensorsInfo()
    }

    private fun loadBasicDeviceInfo() {
        // Device Model
        binding.tvDeviceModel.text = "${Build.BRAND} ${Build.MODEL}"

        // Manufacturer
        binding.tvManufacturer.text = Build.MANUFACTURER

        // Android Version
        binding.tvAndroidVersion.text = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        // SDK Version
        binding.tvSdkVersion.text = Build.VERSION.SDK_INT.toString()

        // Device ID
        binding.tvDeviceId.text = getDeviceId()
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "Unable to retrieve"
        }
    }

    private fun loadNetworkInfo() {
        // Get IP Address
        lifecycleScope.launch {
            try {
                val publicIp = getPublicIpAddress()
                val localIp = getLocalIpAddress()
                
                withContext(Dispatchers.Main) {
                    binding.tvIpAddress.text = if (publicIp != null) {
                        "Public: $publicIp\nLocal: $localIp"
                    } else {
                        "Local: $localIp"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvIpAddress.text = "Unable to retrieve"
                }
            }
        }

        // Get MAC Address
        binding.tvMacAddress.text = getMacAddress()

        // Get WiFi Status
        binding.tvWifiStatus.text = getWifiStatus()
    }

    private suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address.hostAddress?.indexOf(':') == -1) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return "Unknown"
    }

    private fun getMacAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val mac = networkInterface.hardwareAddress
                    if (mac != null) {
                        val macAddress = StringBuilder()
                        for (i in mac.indices) {
                            macAddress.append(String.format("%02X:", mac[i]))
                        }
                        if (macAddress.isNotEmpty()) {
                            macAddress.deleteCharAt(macAddress.length - 1)
                        }
                        return macAddress.toString()
                    }
                }
            }
            "Not available"
        } catch (e: Exception) {
            "Unable to retrieve"
        }
    }

    private fun getWifiStatus(): String {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                getString(R.string.wifi_connected)
            } else {
                getString(R.string.wifi_disconnected)
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }    private fun loadHealthSensorsInfo() {
        // Check for connected smartwatch sensors first
        checkConnectedWearableSensors()
        
        // Phone sensors as fallback
        loadPhoneSensors()

        // Health Connect Support
        checkHealthConnectSupport()
    }    private fun checkConnectedWearableSensors() {
        lifecycleScope.launch {
            try {
                // Check Bluetooth permissions and availability
                if (!::bluetoothAdapter.isInitialized || bluetoothAdapter == null) {
                    binding.tvWearableInfo.text = "Bluetooth not available on this device"
                    return@launch
                }

                if (!bluetoothAdapter.isEnabled) {
                    binding.tvWearableInfo.text = "Bluetooth is disabled. Please enable Bluetooth to detect wearable devices."
                    return@launch
                }

                // Check Bluetooth permissions
                if (!checkBluetoothPermissions()) {
                    binding.tvWearableInfo.text = "Bluetooth permissions required to scan for wearable devices"
                    return@launch
                }

                // Get connected Bluetooth devices
                val connectedDevices = getConnectedBluetoothDevices()
                displayConnectedWearableDevices(connectedDevices)
                
            } catch (e: Exception) {
                binding.tvWearableInfo.text = "Error scanning for wearable devices: ${e.message}"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getConnectedBluetoothDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter.bondedDevices?.filter { device ->
                isWearableDevice(device)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun isWearableDevice(device: BluetoothDevice): Boolean {
        val deviceClass = device.bluetoothClass?.majorDeviceClass
        val deviceName = device.name?.lowercase() ?: ""
        
        // Check device class for wearable devices
        val isWearableClass = deviceClass == android.bluetooth.BluetoothClass.Device.Major.WEARABLE
        
        // Check device name for common wearable device keywords
        val wearableKeywords = listOf(
            "galaxy watch", "fit", "band", "smartwatch", "watch", 
            "fitbit", "garmin", "amazfit", "huawei", "xiaomi mi band",
            "apple watch", "wear os", "samsung galaxy", "gear"
        )
        
        val isWearableName = wearableKeywords.any { keyword ->
            deviceName.contains(keyword)
        }
        
        return isWearableClass || isWearableName
    }

    @SuppressLint("MissingPermission")
    private fun displayConnectedWearableDevices(devices: List<BluetoothDevice>) {
        if (devices.isEmpty()) {
            binding.tvWearableInfo.text = "No wearable devices found.\n" +
                    "• Make sure your wearable device is paired with this phone\n" +
                    "• Check if Bluetooth is enabled on both devices\n" +
                    "• Ensure the wearable device is nearby and connected"
            
            // Show generic sensor info if no specific device is connected
            displayGenericWearableSensorInfo()
            return
        }

        // Display connected wearable devices
        val deviceInfo = StringBuilder()
        deviceInfo.append("📱 Connected Wearable Devices:\n\n")
        
        devices.forEachIndexed { index, device ->
            try {
                deviceInfo.append("${index + 1}. ${device.name ?: "Unknown Device"}\n")
                deviceInfo.append("   MAC: ${device.address}\n")
                deviceInfo.append("   Type: ${getDeviceTypeDescription(device)}\n")
                deviceInfo.append("   Status: Connected\n\n")
            } catch (e: SecurityException) {
                deviceInfo.append("${index + 1}. Device found (permissions required for details)\n\n")
            }
        }
        
        binding.tvWearableInfo.text = deviceInfo.toString()
        
        // Display sensor information based on the first connected device
        val primaryDevice = devices.first()
        displayWearableSensorInfoForDevice(primaryDevice)
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceTypeDescription(device: BluetoothDevice): String {
        val deviceName = device.name?.lowercase() ?: ""
        
        return when {
            deviceName.contains("galaxy watch") -> "Samsung Galaxy Watch Series"
            deviceName.contains("galaxy fit") -> "Samsung Galaxy Fit Series"
            deviceName.contains("gear") -> "Samsung Gear Series"
            deviceName.contains("fitbit") -> "Fitbit Fitness Tracker"
            deviceName.contains("garmin") -> "Garmin Smartwatch"
            deviceName.contains("amazfit") -> "Amazfit Smartwatch"
            deviceName.contains("mi band") -> "Xiaomi Mi Band"
            deviceName.contains("huawei") -> "Huawei Wearable"
            deviceName.contains("apple watch") -> "Apple Watch"
            deviceName.contains("watch") -> "Smartwatch"
            deviceName.contains("band") || deviceName.contains("fit") -> "Fitness Tracker"
            else -> "Wearable Device"
        }
    }

    @SuppressLint("MissingPermission")
    private fun displayWearableSensorInfoForDevice(device: BluetoothDevice) {
        val deviceName = device.name?.lowercase() ?: ""
        
        // Heart Rate Sensor based on device type
        binding.tvHeartRateSensor.text = when {
            deviceName.contains("galaxy watch") || deviceName.contains("galaxy fit") -> {
                "✅ Samsung PPG Heart Rate Sensor\n" +
                        "• Continuous heart rate monitoring\n" +
                        "• Heart rate zones detection\n" +
                        "• Irregular heart rhythm notifications\n" +
                        "• Exercise heart rate tracking\n" +
                        "• Samsung Health integration"
            }
            deviceName.contains("fitbit") -> {
                "✅ Fitbit Heart Rate Sensor\n" +
                        "• PurePulse heart rate technology\n" +
                        "• 24/7 heart rate tracking\n" +
                        "• Heart rate zones\n" +
                        "• Cardio fitness score"
            }
            deviceName.contains("garmin") -> {
                "✅ Garmin Elevate Heart Rate Sensor\n" +
                        "• Wrist-based heart rate monitoring\n" +
                        "• Heart rate variability\n" +
                        "• Performance metrics\n" +
                        "• Recovery advisor"
            }
            else -> {
                "✅ Heart Rate Sensor Available\n" +
                        "• Photoplethysmography (PPG) technology\n" +
                        "• Continuous monitoring\n" +
                        "• Exercise tracking\n" +
                        "• Health app integration"
            }
        }

        // SpO2 Sensor based on device type
        binding.tvSpO2Sensor.text = when {
            deviceName.contains("galaxy watch") -> {
                "✅ Samsung SpO2 Sensor\n" +
                        "• Blood oxygen saturation measurement\n" +
                        "• Sleep breathing pattern analysis\n" +
                        "• Manual and automatic readings\n" +
                        "• Samsung Health integration"
            }
            deviceName.contains("fitbit") -> {
                "✅ Fitbit SpO2 Sensor\n" +
                        "• Blood oxygen variation tracking\n" +
                        "• Sleep score insights\n" +
                        "• Health metrics dashboard"
            }
            else -> {
                "✅ Blood Oxygen Sensor Available\n" +
                        "• SpO2 measurement capability\n" +
                        "• Sleep monitoring support\n" +
                        "• Health tracking integration"
            }
        }

        // Motion Sensors
        binding.tvAccelerometer.text = "✅ Motion Sensors Available\n" +
                "• 3-axis accelerometer\n" +
                "• 3-axis gyroscope\n" +
                "• Advanced step counting\n" +
                "• Activity auto-detection\n" +
                "• Sleep movement tracking\n" +
                "• Fall detection (if supported)"

        // Additional sensors based on device capabilities
        binding.tvAdditionalSensors.text = getAdditionalSensorInfo(deviceName)
    }

    private fun getAdditionalSensorInfo(deviceName: String): String {
        val commonSensors = "🔹 Standard Sensors\n" +
                "   • Ambient light sensor\n" +
                "   • Barometric pressure sensor\n" +
                "   • Temperature sensor\n\n" +
                "🔹 Location & Navigation\n" +
                "   • GPS connectivity (if supported)\n" +
                "   • Distance and pace tracking\n" +
                "   • Route mapping\n\n"

        return when {
            deviceName.contains("galaxy watch") -> {
                commonSensors +
                        "🔹 Samsung-specific Features\n" +
                        "   • Body composition analysis (select models)\n" +
                        "   • ECG monitoring (select models)\n" +
                        "   • Blood pressure monitoring (select models)\n" +
                        "   • Skin temperature monitoring\n" +
                        "   • Stress monitoring\n" +
                        "   • Sleep stage analysis\n\n" +
                        "Note: Feature availability depends on your specific Galaxy Watch model."
            }
            deviceName.contains("fitbit") -> {
                commonSensors +
                        "🔹 Fitbit-specific Features\n" +
                        "   • Active Zone Minutes\n" +
                        "   • Stress management score\n" +
                        "   • Skin temperature variation\n" +
                        "   • Sleep score\n" +
                        "   • Guided breathing sessions\n\n" +
                        "Note: Feature availability depends on your specific Fitbit model."
            }
            else -> {
                commonSensors +
                        "🔹 Advanced Health Monitoring\n" +
                        "   • Stress level detection\n" +
                        "   • Sleep stage analysis\n" +
                        "   • Recovery time estimation\n\n" +
                        "Note: Actual sensor availability depends on your specific wearable device model and manufacturer."
            }
        }
    }

    private fun displayGenericWearableSensorInfo() {
        // Generic wearable device information when no specific device is detected
        binding.tvHeartRateSensor.text = "❓ Heart Rate Sensor\n" +
                "• Connect a wearable device to see specific sensor information\n" +
                "• Most modern wearables include PPG heart rate sensors"
        
        binding.tvSpO2Sensor.text = "❓ Blood Oxygen Sensor\n" +
                "• Connect a compatible wearable device\n" +
                "• Many newer smartwatches include SpO2 sensors"
        
        binding.tvAccelerometer.text = "❓ Motion Sensors\n" +
                "• Connect a wearable device to see motion sensor details\n" +
                "• Standard accelerometer and gyroscope expected"
        
        binding.tvAdditionalSensors.text = "Connect a wearable device via Bluetooth to see detailed sensor information.\n\n" +
                "Supported device types:\n" +
                "• Samsung Galaxy Watch/Fit series\n" +
                "• Fitbit devices\n" +
                "• Garmin smartwatches\n" +
                "• Amazfit devices\n" +
                "• Xiaomi Mi Band\n" +
                "• Other Wear OS devices"
    }

    private fun checkBluetoothPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun loadPhoneSensors() {
        // Step Counter Sensor
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        binding.tvStepCounter.text = if (stepCounter != null) {
            "Phone: ${getString(R.string.sensor_available)}"
        } else {
            "Phone: ${getString(R.string.sensor_not_available)}"
        }

        // Gyroscope
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        binding.tvGyroscope.text = if (gyroscope != null) {
            "Phone: ${getString(R.string.sensor_available)}"
        } else {
            "Phone: ${getString(R.string.sensor_not_available)}"
        }
    }

    private fun checkHealthConnectSupport() {
        try {
            val healthConnectSupported = HealthConnectClient.getSdkStatus(requireContext()) == HealthConnectClient.SDK_AVAILABLE
            binding.tvHealthConnectSupport.text = if (healthConnectSupported) {
                getString(R.string.supported)
            } else {
                getString(R.string.not_supported)
            }
        } catch (e: Exception) {
            binding.tvHealthConnectSupport.text = getString(R.string.not_supported)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = DeviceInfoFragment()
    }
}