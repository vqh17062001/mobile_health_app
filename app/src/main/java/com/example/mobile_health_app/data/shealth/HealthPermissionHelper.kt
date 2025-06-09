package com.example.mobile_health_app.data.shealth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.mobile_health_app.R
import com.samsung.android.sdk.healthdata.*
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult
import com.samsung.android.sdk.healthdata.HealthPermissionManager
import com.samsung.android.sdk.healthdata.HealthResultHolder
import com.samsung.android.sdk.healthdata.HealthConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Helper để connect tới Samsung Health SDK và request các quyền READ_*.
 */
class HealthPermissionHelper(context: Context) {

    companion object {
        private const val TAG = "HealthPermissionHelper"
    }
    // Cờ báo đã bind thành công hay chưa
    private var isConnected = false

    private val store = HealthDataStore(
        context,
        object : HealthDataStore.ConnectionListener {
            override fun onConnected() {
                Log.i(TAG, "✅ Samsung HealthService connected")
                isConnected = true
            }

            override fun onConnectionFailed(error: HealthConnectionErrorResult) {
                Log.e(TAG, "❌ Failed to connect HealthService: $error")
                isConnected = false
            }

            override fun onDisconnected() {
                Log.w(TAG, "⚠ Samsung HealthService disconnected")
                isConnected = false
            }
        }
    ).apply {
        // Khởi bind ngay sau khi tạo
        connectService()
    }


    /**
     * Danh sách các dataType cần quyền READ.
     * 1) Những dataType cần WRITE sẽ được request riêng khi cần.
     *    Ví dụ: khi user muốn ghi weight, ta sẽ request WRITE_WEIGHT.
     *    Việc này giúp tránh request quá nhiều quyền không cần thiết.
     */
    // 2) Những dataType cần READ
    private val dataTypes = listOf(
        HealthConstants.StepCount.HEALTH_DATA_TYPE,
        HealthConstants.HeartRate.HEALTH_DATA_TYPE,
        HealthConstants.BloodPressure.HEALTH_DATA_TYPE,
        HealthConstants.Weight.HEALTH_DATA_TYPE
    )

    init {
        // Bắt đầu bind vào Samsung Health service ngay khi khởi tạo
        store.connectService()
    }

    /**
     * Request runtime permission. Chỉ thực thi khi đã kết nối thành công.
     *
     * @param activity cần để Samsung Health pop dialog
     * @param callback trả về true khi user đã cấp hết, false otherwise
     */
    fun requestPermissions(activity: Activity, callback: (Boolean) -> Unit) {
        if (!isConnected) {
            Log.e(TAG, "Cannot request permissions: not connected to HealthService")
            callback(false)
            return
        }

        val pms = HealthPermissionManager(store)
        // map dataTypes thành PermissionKey
        val keys = dataTypes.map {
            HealthPermissionManager.PermissionKey(it, HealthPermissionManager.PermissionType.READ)
        }.toSet()

        if (keys.isEmpty()) {
            callback(true)
            return
        }

        // Hiện dialog giải thích trước
        AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setMessage("Ứng dụng cần quyền đọc dữ liệu sức khỏe để hoạt động chính xác.")
            .setCancelable(false)
            .setPositiveButton("Cho phép") { _, _ ->
                // Thực sự gọi requestPermissions → trả về HealthResultHolder
                val holder: HealthResultHolder<HealthPermissionManager.PermissionResult> =
                    pms.requestPermissions(keys, activity)

                // Đăng listener để nhận kết quả
                holder.setResultListener(object :
                    HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> {
                    override fun onResult(result: HealthPermissionManager.PermissionResult) {
                        val grantedAll = result.resultMap.values.all { it }
                        Log.d(TAG, "Permission results: ${result.resultMap}")
                        callback(grantedAll)
                    }
                })
            }
            .setNegativeButton("Từ chối") { _, _ ->
                callback(false)
            }
            .show()
    }

    /**
     * Suspend function kiểm tra xem đã có tất cả permissions chưa.
     * Dùng khi bạn muốn đảm bảo trước khi đọc data.
     */
    suspend fun checkPermissions(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) {
            Log.e(TAG, "Cannot check permissions: not connected")
            return@withContext false
        }
        return@withContext try {
            val pms = HealthPermissionManager(store)
            // gom các key vào 1 set, gọi 1 lần
            val keys = dataTypes.map {
                HealthPermissionManager.PermissionKey(it, HealthPermissionManager.PermissionType.READ)
            }.toSet()
            val resultMap = pms.isPermissionAcquired(keys)
            resultMap.values.all { it }
        } catch (e: Exception) {
            Log.e(TAG, "checkPermissions failed", e)
            false
        }
    }

    /**
     * Gọi ở onDestroy của Activity / Fragment để unbind service.
     */
    fun cleanup() {
        if (isConnected) {
            store.disconnectService()
            isConnected = false
        }
    }
}
