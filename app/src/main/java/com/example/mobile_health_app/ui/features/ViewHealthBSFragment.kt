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
import com.example.mobile_health_app.data.hconnect.HealthConnectManager
import com.example.mobile_health_app.viewmodel.HealthConnectViewModel
import com.example.mobile_health_app.databinding.FragmentViewHealthBsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.time.Instant
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
                showError("Bạn phải cấp quyền Health Connect để xem bước chân")
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

    private fun fetchHealthData() {
        showLoading(true)
        val now = Instant.now()
        val yesterday = now.minusSeconds(24 * 3600)
        healthConnectViewModel.loadSteps(yesterday, now)
        healthConnectViewModel.loadDistance(yesterday, now)
        healthConnectViewModel.loadTotalCaloriesBurned(yesterday, now)
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
