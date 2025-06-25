package com.example.mobile_health_app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mobile_health_app.Service.HealthSyncManager

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed - checking for health sync configuration")
            
            // Kiểm tra và khởi chạy service đồng bộ nếu có cấu hình
            HealthSyncManager.checkAndStartSyncService(context)
        }
    }
}