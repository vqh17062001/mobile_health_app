package com.example.mobile_health_app.ui.features

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.NetworkInterface

class DeviceInfoFragment : BottomSheetDialogFragment() {

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
        bluetoothAdapter = bluetoothManager.adapter        // Load device information
        loadDeviceInformation()

        // Set refresh button click listener
        binding.btnRefreshInfo.setOnClickListener {
            loadDeviceInformation()
            Toast.makeText(requireContext(), getString(R.string.device_info_refreshed), Toast.LENGTH_SHORT).show()
        }        // Set close button click listener
        binding.btnClose.setOnClickListener {
            // Navigate back to previous fragment
            dismiss()
        }
    }    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val sheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            
            sheet?.let { view ->
                val behavior = BottomSheetBehavior.from(view)
                
                // Set initial height to 90% of screen
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val peekHeight = (screenHeight * 0.95).toInt()
                
                view.layoutParams.height = peekHeight
                behavior.peekHeight = peekHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isHideable = true
                behavior.skipCollapsed = false
                
                // Handle drag to dismiss
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dismiss()
                            }
                            BottomSheetBehavior.STATE_DRAGGING -> {
                                // Optional: Add haptic feedback when dragging starts
                                bottomSheet.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    }
                    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Optional: Add fade effect based on slide offset
                        bottomSheet.alpha = (slideOffset + 1f).coerceIn(0.3f, 1f)
                    }
                })
            }
        }
        
        return dialog
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
    }    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            getString(R.string.unable_to_retrieve)
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
                        "${getString(R.string.public_ip)}: $publicIp\n${getString(R.string.local_ip)}: $localIp"
                    } else {
                        "${getString(R.string.local_ip)}: $localIp"
                    }
                }            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvIpAddress.text = getString(R.string.unable_to_retrieve)
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
                        return address.hostAddress ?: getString(R.string.unknown_status)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return getString(R.string.unknown_status)
    }    private fun getMacAddress(): String {
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
            getString(R.string.not_available)
        } catch (e: Exception) {
            getString(R.string.unable_to_retrieve)
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
            getString(R.string.unknown_status)
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
                    binding.tvWearableInfo.text = getString(R.string.bluetooth_not_available)
                    return@launch
                }

                if (!bluetoothAdapter.isEnabled) {
                    binding.tvWearableInfo.text = getString(R.string.bluetooth_disabled)
                    return@launch
                }

                // Check Bluetooth permissions
                if (!checkBluetoothPermissions()) {
                    binding.tvWearableInfo.text = getString(R.string.bluetooth_permissions_required)
                    return@launch
                }

                // Get connected Bluetooth devices
                val connectedDevices = getConnectedBluetoothDevices()
                displayConnectedWearableDevices(connectedDevices)
                
            } catch (e: Exception) {
                binding.tvWearableInfo.text = getString(R.string.wearable_scan_error, e.message ?: getString(R.string.unknown_status))
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
    }    @SuppressLint("MissingPermission")
    private fun displayConnectedWearableDevices(devices: List<BluetoothDevice>) {
        if (devices.isEmpty()) {
            binding.tvWearableInfo.text = getString(R.string.no_wearable_devices)
            
            // Show generic sensor info if no specific device is connected
            displayGenericWearableSensorInfo()
            return
        }

        // Display connected wearable devices
        val deviceInfo = StringBuilder()
        deviceInfo.append(getString(R.string.connected_wearable_devices)).append("\n\n")
        
        devices.forEachIndexed { index, device ->
            try {
                deviceInfo.append("${index + 1}. ${device.name ?: getString(R.string.unknown_device)}\n")
                deviceInfo.append("   MAC: ${device.address}\n")
                deviceInfo.append("   ${getString(R.string.device_type)}: ${getDeviceTypeDescription(device)}\n")
                deviceInfo.append("   ${getString(R.string.device_status)}: ${getString(R.string.device_status_connected)}\n\n")
            } catch (e: SecurityException) {
                deviceInfo.append("${index + 1}. ${getString(R.string.device_permissions_required)}\n\n")
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
            deviceName.contains("galaxy watch") -> getString(R.string.samsung_galaxy_watch_series)
            deviceName.contains("galaxy fit") -> getString(R.string.samsung_galaxy_fit_series)
            deviceName.contains("gear") -> getString(R.string.samsung_gear_series)
            deviceName.contains("fitbit") -> getString(R.string.fitbit_fitness_tracker)
            deviceName.contains("garmin") -> getString(R.string.garmin_smartwatch)
            deviceName.contains("amazfit") -> getString(R.string.amazfit_smartwatch)
            deviceName.contains("mi band") -> getString(R.string.xiaomi_mi_band)
            deviceName.contains("huawei") -> getString(R.string.huawei_wearable)
            deviceName.contains("apple watch") -> getString(R.string.apple_watch)
            deviceName.contains("watch") -> getString(R.string.smartwatch)
            deviceName.contains("band") || deviceName.contains("fit") -> getString(R.string.fitness_tracker)
            else -> getString(R.string.wearable_device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun displayWearableSensorInfoForDevice(device: BluetoothDevice) {
        val deviceName = device.name?.lowercase() ?: ""
        
        // Heart Rate Sensor based on device type
        binding.tvHeartRateSensor.text = when {
            deviceName.contains("galaxy watch") || deviceName.contains("galaxy fit") -> {
                getString(R.string.samsung_ppg_heart_rate)
            }
            deviceName.contains("fitbit") -> {
                getString(R.string.fitbit_heart_rate)
            }
            deviceName.contains("garmin") -> {
                getString(R.string.garmin_heart_rate)
            }
            else -> {
                getString(R.string.generic_heart_rate_sensor)
            }
        }

        // SpO2 Sensor based on device type
        binding.tvSpO2Sensor.text = when {
            deviceName.contains("galaxy watch") -> {
                getString(R.string.samsung_spo2_sensor)
            }
            deviceName.contains("fitbit") -> {
                getString(R.string.fitbit_spo2_sensor)
            }
            else -> {
                getString(R.string.generic_spo2_sensor)
            }
        }

        // Motion Sensors
        binding.tvAccelerometer.text = getString(R.string.motion_sensors_available)

        // Additional sensors based on device capabilities
        binding.tvAdditionalSensors.text = getAdditionalSensorInfo(deviceName)
    }

    private fun getAdditionalSensorInfo(deviceName: String): String {
        val commonSensors = getString(R.string.common_sensors_info)

        return when {
            deviceName.contains("galaxy watch") -> {
                commonSensors + "\n\n" + getString(R.string.samsung_specific_features)
            }
            deviceName.contains("fitbit") -> {
                commonSensors + "\n\n" + getString(R.string.fitbit_specific_features)
            }
            else -> {
                commonSensors + "\n\n" + getString(R.string.generic_advanced_monitoring)
            }
        }
    }

    private fun displayGenericWearableSensorInfo() {
        // Generic wearable device information when no specific device is detected
        binding.tvHeartRateSensor.text = getString(R.string.heart_rate_sensor_generic)
        binding.tvSpO2Sensor.text = getString(R.string.spo2_sensor_generic)
        binding.tvAccelerometer.text = getString(R.string.motion_sensor_generic)
        binding.tvAdditionalSensors.text = getString(R.string.connect_wearable_message)
    }    private fun loadPhoneSensors() {
        // Step Counter Sensor
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        binding.tvStepCounter.text = if (stepCounter != null) {
            "${getString(R.string.phone_prefix)} ${getString(R.string.sensor_available)}"
        } else {
            "${getString(R.string.phone_prefix)} ${getString(R.string.sensor_not_available)}"
        }

        // Gyroscope
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        binding.tvGyroscope.text = if (gyroscope != null) {
            "${getString(R.string.phone_prefix)} ${getString(R.string.sensor_available)}"
        } else {
            "${getString(R.string.phone_prefix)} ${getString(R.string.sensor_not_available)}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = DeviceInfoFragment()
    }
}