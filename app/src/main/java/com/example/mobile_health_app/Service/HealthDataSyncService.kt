package com.example.mobile_health_app.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.hconnect.HealthConnectRepository
import com.example.mobile_health_app.data.model.Metadata
import com.example.mobile_health_app.data.model.Reading
import com.example.mobile_health_app.data.model.SensorReading
import com.example.mobile_health_app.data.model.ValueType
import com.example.mobile_health_app.data.repository.SensorReadingRepository
import com.example.mobile_health_app.ui.features.SyncHealthConfigFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.pm.ServiceInfo

class HealthDataSyncService : Service() {

    companion object {
        private const val TAG = "HealthDataSyncService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "health_sync_channel"
        private const val SYNC_INTERVAL_MS =   60 * 1000L // 30 s
        
        // Keys for sync configuration
        private const val SYNC_PREFS_NAME = "health_sync_config"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SYNC_ACTIVITY = "sync_activity"
        private const val KEY_SYNC_SLEEP = "sync_sleep"
        private const val KEY_SYNC_HEART_RATE = "sync_heart_rate"
        private const val KEY_SYNC_SPO2 = "sync_spo2"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }

    private lateinit var healthConnectRepository: HealthConnectRepository
    private lateinit var sensorReadingRepository: SensorReadingRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null
    private var currentUserId: ObjectId? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HealthDataSyncService created")
        
        healthConnectRepository = HealthConnectRepository(this)
        sensorReadingRepository = SensorReadingRepository()
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HealthDataSyncService started")
        
        // Check if sync is enabled and get user configuration
        if (isSyncEnabled()) {
            startForegroundService()
            startPeriodicSync()
        } else {
            Log.d(TAG, "Sync not enabled or no configuration found")
            stopSelf()
        }
        
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HealthDataSyncService destroyed")
        
        // Cancel all running jobs
        serviceScope.cancel()
        
        // Remove periodic sync
        syncRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Data Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Syncing health data with server"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Syncing health data...")
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Data Sync")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun isSyncEnabled(): Boolean {
        val sharedPrefs = getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString(KEY_USER_ID, null)
        
        if (userId != null) {
            currentUserId = try {
                ObjectId(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid user ID: $userId")
                return false
            }
            
            // Check if at least one sync option is enabled
            val hasActivity = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userId), false)
            val hasSleep = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SLEEP, userId), false)
            val hasHeartRate = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userId), false)
            val hasSpO2 = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SPO2, userId), false)
            
            return hasActivity || hasSleep || hasHeartRate || hasSpO2
        }
        
        return false
    }

    private fun getKeyForUser(baseKey: String, userId: String): String {
        return "${baseKey}_$userId"
    }

    private fun startPeriodicSync() {
        syncRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Starting periodic sync")
                performSync()
                // Schedule next sync
                handler.postDelayed(this, SYNC_INTERVAL_MS)
            }
        }
        
        // Start first sync immediately
        handler.post(syncRunnable!!)
    }

    private fun performSync() {
        currentUserId?.let { userId ->
            serviceScope.launch {
                try {
                    val userIdString = userId.toString()
                    val userid = bsonObjectIdToValue(userIdString)
                    Log.d(TAG, "Performing sync for user: $userid")
                    updateNotification("Syncing health data...")
                    
                    val sharedPrefs = getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)

                    // Get last sync time
                    val lastSyncTime = getLastSyncTime(userIdString).minusSeconds(20) // trừ delay
                    val currentTime = Instant.now()
                    
                    var syncCount = 0
                    
                    // Sync Activity Data (Steps, Distance, Calories)
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userid), false)) {
                        syncCount += syncActivityData(userId, lastSyncTime, currentTime)
                    }
                    
                    // Sync Sleep Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SLEEP,userid), false)) {
                        syncCount += syncSleepData(userId,lastSyncTime , currentTime)
                    }
                    
                    // Sync Heart Rate Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userid),false)) {
                        syncCount += syncHeartRateData(userId, lastSyncTime, currentTime)
                    }
                    
                    // Sync SpO2 Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SPO2, userid), false)) {
                        syncCount += syncSpO2Data(userId, lastSyncTime, currentTime)
                    }
                    
                    // Update last sync time
                    saveLastSyncTime(userIdString, currentTime)
                    
                    Log.d(TAG, "Sync completed. Records synced: $syncCount")
                    updateNotification("Sync completed. $syncCount records synced")
                    
                    // Show success notification briefly
                    delay(3000)
                    updateNotification("Health data sync active")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed", e)
                    updateNotification("Sync failed: ${e.message}")
                }
            }
        }
    }

    fun bsonObjectIdToValue(s: String): String {
        return if (s.startsWith("BsonObjectId(") && s.endsWith(")")) {
            s.removePrefix("BsonObjectId(").removeSuffix(")")
        } else s
    }

    private suspend fun syncActivityData(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            // Sync Steps
            val stepsRecords = healthConnectRepository.getSteps(from, to)
            for (record in stepsRecords) {
                val readings = listOf(
                    Reading("steps", ValueType.IntValue(record.count.toInt())),
                    Reading("type", ValueType.StringValue("steps"))
                )
                val sensorReading = SensorReading(
                    timestamp = record.startTime,
                    metadata = Metadata(
                        userId = userId,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "activity"
                    ),
                    readings = readings
                )
                if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                    syncCount++
                }
            }
            
            // Sync Distance
            val distanceRecords = healthConnectRepository.getDistance(from, to)
            for (record in distanceRecords) {
                val readings = listOf(
                    Reading("distance_meters", ValueType.DoubleValue(record.distance.inMeters)),
                    Reading("type", ValueType.StringValue("distance"))
                )
                val sensorReading = SensorReading(
                    timestamp = record.startTime,
                    metadata = Metadata(
                        userId = userId,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "activity"
                    ),
                    readings = readings
                )
                if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                    syncCount++
                }
            }
            
            // Sync Calories
            val caloriesRecords = healthConnectRepository.getTotalCaloriesBurned(from, to)
            for (record in caloriesRecords) {
                val readings = listOf(
                    Reading("calories", ValueType.DoubleValue(record.energy.inKilocalories)),
                    Reading("type", ValueType.StringValue("calories"))
                )
                val sensorReading = SensorReading(
                    timestamp = record.startTime,
                    metadata = Metadata(
                        userId = userId,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "activity"
                    ),
                    readings = readings
                )
                if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                    syncCount++
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync activity data", e)
        }
        
        return syncCount
    }

    private suspend fun syncSleepData(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            val sleepRecords = healthConnectRepository.getSleepSessions(from, to)
            for (record in sleepRecords) {
                val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                val readings = listOf(
                    Reading("sleep_duration_minutes", ValueType.IntValue(durationMinutes.toInt())),
                    Reading("start_time", ValueType.StringValue(record.startTime.toString())),
                    Reading("end_time", ValueType.StringValue(record.endTime.toString())),
                    Reading("type", ValueType.StringValue("sleep"))
                )
                val sensorReading = SensorReading(
                    timestamp = record.startTime,
                    metadata = Metadata(
                        userId = userId,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "sleep"
                    ),
                    readings = readings
                )
                if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                    syncCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync sleep data", e)
        }
        
        return syncCount
    }

    private suspend fun syncHeartRateData(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            val heartRateRecords = healthConnectRepository.getHeartRates(from, to)
            for (record in heartRateRecords) {
                for (sample in record.samples) {
                    val readings = listOf(
                        Reading("heart_rate_bpm", ValueType.IntValue(sample.beatsPerMinute.toInt())),
                        Reading("time", ValueType.StringValue(sample.time.toString())),
                        Reading("type", ValueType.StringValue("heart_rate"))
                    )
                    val sensorReading = SensorReading(
                        timestamp = sample.time,
                        metadata = Metadata(
                            userId = userId,
                            deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                            sensorType = "heart_rate"
                        ),
                        readings = readings
                    )
                    if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                        syncCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync heart rate data", e)
        }
        
        return syncCount
    }

    private suspend fun syncSpO2Data(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            val spo2Records = healthConnectRepository.getOxygenSaturation(from, to)
            for (record in spo2Records) {
                val readings = listOf(
                    Reading("spo2_percentage", ValueType.DoubleValue(record.percentage.value)),
                    Reading("time", ValueType.StringValue(record.time.toString())),
                    Reading("type", ValueType.StringValue("spo2"))
                )
                val sensorReading = SensorReading(
                    timestamp = record.time,
                    metadata = Metadata(
                        userId = userId,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "spo2"
                    ),
                    readings = readings
                )
                if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                    syncCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync SpO2 data", e)
        }
        
        return syncCount
    }

    private fun getLastSyncTime(userId: String): Instant {
        val sharedPrefs = getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
        val lastSyncMillis = sharedPrefs.getLong(getKeyForUser(KEY_LAST_SYNC_TIME, userId), 0L)
        
        return if (lastSyncMillis > 0) {
            Instant.ofEpochMilli(lastSyncMillis)
        } else {
            // If no previous sync, sync data from 24 hours ago
            Instant.now().minusSeconds(24 * 60 * 60)
        }
    }

    private fun saveLastSyncTime(userId: String, time: Instant) {
        val sharedPrefs = getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong(getKeyForUser(KEY_LAST_SYNC_TIME, userId), time.toEpochMilli())
            .apply()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}