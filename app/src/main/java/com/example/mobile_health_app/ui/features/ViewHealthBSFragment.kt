package com.example.mobile_health_app.ui.features

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.hconnect.HealthConnectManager
import com.example.mobile_health_app.viewmodel.HealthConnectViewModel
import com.example.mobile_health_app.databinding.FragmentViewHealthBsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ViewHealthBSFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentViewHealthBsBinding? = null
    private val binding get() = _binding!!

    // 1. Shared ViewModel do bạn cung cấp
    private val healthConnectViewModel: HealthConnectViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    // 2. Khai báo manager (chứa permissions, client, contract)
    private lateinit var manager: HealthConnectManager

    // 3. Launcher chỉ dùng contract từ manager, không biết gì thêm
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Manager với context của fragment
        manager = HealthConnectManager(requireContext())

        // Đăng ký launcher dùng đúng contract từ manager
        permissionLauncher = registerForActivityResult(
            manager.requestPermissionsActivityContract()
        ) { grantedPermissions ->
            if (grantedPermissions.containsAll(manager.permissions)) {
                fetchHealthData()
            } else {
                showError(getString(R.string.health_connect_permission_required))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewHealthBsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút Đóng
        binding.btnClose.setOnClickListener { dismiss() }

        // Setup all health data observers
        setupHealthDataObservers()
        
        // 4. Check hoặc request permissions, rồi fetch
        viewLifecycleOwner.lifecycleScope.launch {
            if (manager.hasAllPermissions(manager.permissions)) {
                fetchHealthData()
            } else {
                permissionLauncher.launch(manager.permissions)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dlg = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dlg.setOnShowListener {
            val sheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let { view ->
                val behavior = BottomSheetBehavior.from(view)
                val height = requireActivity().window.decorView.height.takeIf { it > 0 }
                    ?: resources.displayMetrics.heightPixels
                view.layoutParams.height = (height * 0.9).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dlg
    }

    /**
     * Sets up observers for all health data LiveData streams from the ViewModel
     */
    private fun setupHealthDataObservers() {
        // Observe LiveData bước chân từ ViewModel
        healthConnectViewModel.steps.observe(viewLifecycleOwner) { records ->
            showLoading(false)
            val total = records.sumOf { it.count.toLong() }
            binding.tvStepCount.text = "$total"
        }
        
        // Observe LiveData khoảng cách từ ViewModel
        healthConnectViewModel.distance.observe(viewLifecycleOwner) { records ->
            val totalMeters = records.sumOf { it.distance.inMeters }
            val formattedDistance = if (totalMeters >= 1000) {
                String.format("%.2f km", totalMeters / 1000)
            } else {
                String.format("%d m", totalMeters.roundToInt())
            }
            binding.tvDistanceValue.text = formattedDistance
        }
        
        // Observe LiveData calories từ ViewModel
        healthConnectViewModel.totalCaloriesBurned.observe(viewLifecycleOwner) { records ->
            val totalCalories = records.sumOf { it.energy.inKilocalories }
            binding.tvCaloriesValue.text = String.format("%.0f kcal", totalCalories)
        }

        // Setup specialized observer for sleep data which is more complex
        healthConnectViewModel.sleepSessions.observe(viewLifecycleOwner) { records ->
            if (records.isNotEmpty()) {
                // Sort sleep sessions by start time
                val sortedSessions = records.sortedBy { it.startTime }

                // Calculate total sleep duration in milliseconds
                var totalSleepDuration = 0L
                sortedSessions.forEach { session ->
                    totalSleepDuration += session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
                }

                // Convert to hours and minutes
                val hours = TimeUnit.MILLISECONDS.toHours(totalSleepDuration)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(totalSleepDuration) % 60
                binding.tvSleepDuration.text = "${hours}h ${minutes}m"

                // Get earliest bedtime and latest wake time
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val earliestBedtime = LocalDateTime.ofInstant(
                    sortedSessions.first().startTime,
                    ZoneId.systemDefault()
                ).format(formatter)

                val latestWakeup = LocalDateTime.ofInstant(
                    sortedSessions.last().endTime,
                    ZoneId.systemDefault()
                ).format(formatter)

                binding.tvBedTime.text = earliestBedtime
                binding.tvWakeTime.text = latestWakeup

                // Basic sleep quality assessment based on duration
                val sleepQuality = when {
                    hours >= 8 -> getString(R.string.good_sleep_duration)
                    hours >= 6 -> getString(R.string.adequate_sleep)
                    else -> getString(R.string.insufficient_sleep)
                }
                binding.tvSleepQuality.text = sleepQuality
            } else {
                binding.tvSleepDuration.text = "0h 0m"
                binding.tvBedTime.text = "--:--"
                binding.tvWakeTime.text = "--:--"
                binding.tvSleepQuality.text = getString(R.string.no_sleep_data)
            }
        }

        // Observe LiveData for heart rate from ViewModel
        healthConnectViewModel.heartRates.observe(viewLifecycleOwner) { records ->
            if (records.isNotEmpty()) {
                // Sort heart rate records by time
                val sortedRecords = records.sortedBy { it.endTime }
                
                // Get the most recent heart rate (for current heart rate)
                val latestRecord = sortedRecords.last()
                val latestHeartRate = latestRecord.samples.maxByOrNull { it.time }?.beatsPerMinute?.toInt() ?: 0
                binding.tvHeartRate.text = latestHeartRate.toString()
                
                // Find min and max heart rate
                var minHeartRate = Int.MAX_VALUE
                var maxHeartRate = Int.MIN_VALUE
                var totalHeartRate = 0
                var sampleCount = 0
                
                records.forEach { record ->
                    record.samples.forEach { sample ->
                        val rate = sample.beatsPerMinute.toInt()
                        minHeartRate = minOf(minHeartRate, rate)
                        maxHeartRate = maxOf(maxHeartRate, rate)
                        totalHeartRate += rate
                        sampleCount++
                    }
                }
                
                // Calculate average heart rate
                val avgHeartRate = if (sampleCount > 0) totalHeartRate / sampleCount else 0
                
                // Update UI
                binding.tvMinHeartRate.text = "$minHeartRate ${getString(R.string.bpm_suffix)}"
                binding.tvMaxHeartRate.text = "$maxHeartRate ${getString(R.string.bpm_suffix)}"
                binding.tvAvgHeartRate.text = "$avgHeartRate ${getString(R.string.bpm_suffix)}"
                
                // Simple heart rate status assessment
                val heartRateStatus = when {
                    latestHeartRate < 60 -> getString(R.string.resting_heart_rate)
                    latestHeartRate in 60..100 -> getString(R.string.normal_heart_rate)
                    latestHeartRate in 101..160 -> getString(R.string.elevated_heart_rate)
                    else -> getString(R.string.high_heart_rate)
                }
                binding.tvHeartRateStatus.text = heartRateStatus
            } else {
                binding.tvHeartRate.text = "--"
                binding.tvMinHeartRate.text = "-- ${getString(R.string.bpm_suffix)}"
                binding.tvMaxHeartRate.text = "-- ${getString(R.string.bpm_suffix)}"
                binding.tvAvgHeartRate.text = "-- ${getString(R.string.bpm_suffix)}"
                binding.tvHeartRateStatus.text = getString(R.string.no_heart_rate_data)
            }
        }
        
        // Add observer for blood oxygen data
        healthConnectViewModel.oxygenSaturation.observe(viewLifecycleOwner) { records ->
            if (records.isNotEmpty()) {
                // Sort oxygen saturation records by time
                val sortedRecords = records.sortedBy { it.time }
                
                // Get the most recent SpO2 reading
                val latestRecord = sortedRecords.last()
                val latestSpO2 = latestRecord.percentage
                binding.tvSpO2.text = latestSpO2.toString()
                
                // Find min and max SpO2
                var minSpO2 = 100
                var maxSpO2 = 0
                var totalSpO2 = 0
                var sampleCount = 0
                
                records.forEach { record ->
                    val spO2 = record.percentage.value.toInt()
                    minSpO2 = minOf(minSpO2, spO2)
                    maxSpO2 = maxOf(maxSpO2, spO2)
                    totalSpO2 += spO2
                    sampleCount++
                }
                
                // Calculate average SpO2
                val avgSpO2 = if (sampleCount > 0) totalSpO2 / sampleCount else 0
                
                // Update UI
                binding.tvMinSpO2.text = "$minSpO2 %"
                binding.tvMaxSpO2.text = "$maxSpO2 %"
                binding.tvAvgSpO2.text = "$avgSpO2 %"
                
                // SpO2 status assessment
                val spO2Status = when {
                    latestSpO2.value.toInt() >= 95 -> getString(R.string.normal_oxygen_level)
                    latestSpO2.value.toInt() in 90..94 -> getString(R.string.slightly_below_normal)
                    latestSpO2.value.toInt() < 90 -> getString(R.string.low_oxygen_level)
                    else -> getString(R.string.invalid_reading)
                }
                binding.tvSpO2Status.text = spO2Status
            } else {
                binding.tvSpO2.text = "--"
                binding.tvMinSpO2.text = "-- %"
                binding.tvMaxSpO2.text = "-- %"
                binding.tvAvgSpO2.text = "-- %"
                binding.tvSpO2Status.text = getString(R.string.no_spo2_data)
            }
        }
    }

    private fun fetchHealthData() {
        showLoading(true)
        val now = Instant.now()
        val yesterday = now.minusSeconds(24 * 3600)
        val sixdaysAgo = now.minusSeconds(6 * 24 * 3600)
        healthConnectViewModel.loadSteps(yesterday, now)
        healthConnectViewModel.loadDistance(yesterday, now)
        healthConnectViewModel.loadTotalCaloriesBurned(yesterday, now)
        healthConnectViewModel.loadSleepSessions(yesterday, now)
        healthConnectViewModel.loadHeartRates(yesterday, now)
        healthConnectViewModel.loadOxygenSaturation(yesterday, now)  // Add this line to load SpO2 data
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBarHealth.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.tvErrorMessage.visibility = View.GONE
    }

    private fun showError(msg: String) {
        showLoading(false)
        binding.tvErrorMessage.apply {
            text = msg
            visibility = View.VISIBLE
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val TAG = "ViewHealthBottomSheet"
        @JvmStatic fun newInstance() = ViewHealthBSFragment()
        @JvmStatic fun e(tag: String, message: String, e: Exception) {
        }
    }
}
