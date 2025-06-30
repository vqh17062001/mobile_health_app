package com.example.mobile_health_app.Service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class HealthSyncManager {
    
    companion object {
        private const val TAG = "HealthSyncManager"
        private const val SYNC_PREFS_NAME = "health_sync_config"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SYNC_ACTIVITY = "sync_activity"
        private const val KEY_SYNC_SLEEP = "sync_sleep"
        private const val KEY_SYNC_HEART_RATE = "sync_heart_rate"
        private const val KEY_SYNC_SPO2 = "sync_spo2"
        private const val KEY_SYNC_EXERCISE = "sync_exercise"
        
        /**
         * Kiểm tra và khởi chạy service đồng bộ nếu có cấu hình
         */
        fun checkAndStartSyncService(context: Context) {
            if (hasValidSyncConfig(context)) {
                startSyncService(context)
            } else {
                Log.d(TAG, "No valid sync configuration found")
            }
        }
        
        /**
         * Khởi chạy service đồng bộ
         */
        fun startSyncService(context: Context) {
            try {
                val intent = Intent(context, HealthDataSyncService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
                
                Log.d(TAG, "Health sync service started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sync service: ${e.message}")
            }
        }
        
        /**
         * Dừng service đồng bộ
         */
        fun stopSyncService(context: Context) {
            try {
                val intent = Intent(context, HealthDataSyncService::class.java)
                context.stopService(intent)
                Log.d(TAG, "Health sync service stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop sync service: ${e.message}")
            }
        }
        
        /**
         * Kiểm tra xem có cấu hình đồng bộ hợp lệ hay không
         */
        private fun hasValidSyncConfig(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
            val userId = sharedPrefs.getString(KEY_USER_ID, null)
            
            if (userId.isNullOrEmpty()) {
                Log.d(TAG, "No user ID found in sync config")
                return false
            }
            
            // Kiểm tra xem có ít nhất một loại dữ liệu được bật để đồng bộ không
            val hasActivity = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userId), false)
            val hasSleep = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SLEEP, userId), false)
            val hasHeartRate = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userId), false)
            val hasSpO2 = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SPO2, userId), false)
            val hasExercise = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_EXERCISE, userId), false)
            
            val hasAnySyncEnabled = hasActivity || hasSleep || hasHeartRate || hasSpO2 || hasExercise
            
            Log.d(TAG, "Sync config check - UserId: $userId, Activity: $hasActivity, Sleep: $hasSleep, HeartRate: $hasHeartRate, SpO2: $hasSpO2, Exercise: $hasExercise")
            
            return hasAnySyncEnabled
        }
        
        /**
         * Tạo key cho user cụ thể
         */
        private fun getKeyForUser(baseKey: String, userId: String): String {
            return "${baseKey}_$userId"
        }
        
        /**
         * Khởi động lại service đồng bộ sau khi cập nhật cấu hình
         */
        fun restartSyncService(context: Context) {
            Log.d(TAG, "Restarting sync service...")
            stopSyncService(context)
            
            // Delay một chút trước khi khởi chạy lại
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                checkAndStartSyncService(context)
            }, 1000)
        }
    }
}