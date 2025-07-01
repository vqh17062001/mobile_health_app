package com.example.mobile_health_app.Service

import android.annotation.SuppressLint
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
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.example.mobile_health_app.data.repository.DeviceRepository

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
        private const val KEY_SYNC_EXERCISE = "sync_exercise"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        
        // Initial sync offset when service starts/restarts
        private const val INITIAL_SYNC_OFFSET_HOURS = 24L // 24 hours for general data
        private const val SLEEP_SYNC_OFFSET_HOURS = 48L // 48 hours for sleep data (longer period)
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
            val hasExercise = sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_EXERCISE, userId), false)
            
            return hasActivity || hasSleep || hasHeartRate || hasSpO2 || hasExercise
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

    /**
     * Calculate the sync start time with offset for service startup/restart
     * @param lastSyncTime The last recorded sync time
     * @param isServiceStartup Whether this is a service startup (first sync after start)
     * @param dataType The type of data being synced (for different offset strategies)
     * @return Adjusted sync start time
     */
    private fun getAdjustedSyncTime(
        lastSyncTime: Instant, 
        isServiceStartup: Boolean = true, 
        dataType: String = "general"
    ): Instant {
        return if (isServiceStartup) {
            val offsetHours = when (dataType) {
                "sleep" -> SLEEP_SYNC_OFFSET_HOURS
                else -> INITIAL_SYNC_OFFSET_HOURS
            }
            lastSyncTime.minusSeconds(offsetHours * 60 * 60)
        } else {
            // For regular periodic syncs, use minimal offset
            lastSyncTime.minusSeconds(20)
        }
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
                    val baseSyncTime = getLastSyncTime(userIdString)
                    val currentTime = Instant.now()
                    
                    // Check if this is the first sync after service start (simple heuristic)
                    val isServiceStartup = syncRunnable?.let { 
                        handler.hasCallbacks(it) 
                    } ?: true
                    
                    var syncCount = 0
                    
                    // Sync Activity Data (Steps, Distance, Calories)
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_ACTIVITY, userid), false)) {
                        val activitySyncTime = getAdjustedSyncTime(baseSyncTime, isServiceStartup, "activity")
                        syncCount += syncActivityData(userId, activitySyncTime, currentTime)
                    }
                    
                    // Sync Sleep Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SLEEP,userid), false)) {
                        val sleepSyncTime = getAdjustedSyncTime(baseSyncTime, isServiceStartup, "sleep")
                        syncCount += syncSleepData(userId, sleepSyncTime, currentTime)
                    }
                    
                    // Sync Heart Rate Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_HEART_RATE, userid),false)) {
                        val heartRateSyncTime = getAdjustedSyncTime(baseSyncTime, isServiceStartup, "heart_rate")
                        syncCount += syncHeartRateData(userId, heartRateSyncTime, currentTime)
                    }
                    
                    // Sync SpO2 Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_SPO2, userid), false)) {
                        val spo2SyncTime = getAdjustedSyncTime(baseSyncTime, isServiceStartup, "spo2")
                        syncCount += syncSpO2Data(userId, spo2SyncTime, currentTime)
                    }
                    
                    // Sync Exercise Data
                    if (sharedPrefs.getBoolean(getKeyForUser(KEY_SYNC_EXERCISE, userid), false)) {
                        val exerciseSyncTime = getAdjustedSyncTime(baseSyncTime, isServiceStartup, "exercise")
                        syncCount += syncExerciseData(userId, exerciseSyncTime.minusSeconds(60*60*24), currentTime)
                    }
                    updateDeviceslastSyncTime(userId, currentTime)
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

    private suspend fun CoroutineScope.updateDeviceslastSyncTime(
        userId: org.mongodb.kbson.ObjectId,
        currentTime: java.time.Instant
    ) {
       var deviceRepository = com.example.mobile_health_app.data.repository.DeviceRepository()
        // Update the last sync time for the device

        deviceRepository.updateDeviceStatus(
          deviceId =  android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
          ownerId =  userId,
          status =  "online"
        )
        Log.d(TAG, "Updating last sync time for user: $userId to $currentTime")
    }



    fun bsonObjectIdToValue(s: String): String {
        return if (s.startsWith("BsonObjectId(") && s.endsWith(")")) {
            s.removePrefix("BsonObjectId(").removeSuffix(")")
        } else s
    }

    @SuppressLint("HardwareIds")
    private suspend fun syncActivityData(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            // Get existing activity records from database to check for duplicates
            val existingActivityRecords = sensorReadingRepository.getSensorReadings(
                userId = userId,
                deviceId = null,
                from = from,
                to = to
            ).filter { sensorReading ->
                sensorReading.metadata.sensorType == "activity"
            }
            
            // Extract existing timestamps for each activity type
            val existingStepsTimestamps = existingActivityRecords.filter { sensorReading ->
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "steps" }
            }.map { it.timestamp }.toSet()
            
            val existingDistanceTimestamps = existingActivityRecords.filter { sensorReading ->
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "distance" }
            }.map { it.timestamp }.toSet()
            
            val existingCaloriesTimestamps = existingActivityRecords.filter { sensorReading ->
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "calories" }
            }.map { it.timestamp }.toSet()
            
            // Sync Steps
            val stepsRecords = healthConnectRepository.getSteps(from, to)
            for (record in stepsRecords) {
                val readings = listOf(
                    Reading("steps", ValueType.IntValue(record.count.toInt())),
                    Reading("type", ValueType.StringValue("steps"))
                )
                
                if (!existingStepsTimestamps.contains(record.startTime)) {
                    // Insert new record
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
                        Log.d(TAG, "Synced new steps record with timestamp: ${record.startTime}")
                    }
                } else {
                    // Update existing record
                    if (sensorReadingRepository.updateSensorReading(
                        userId = userId,
                        timestamp = record.startTime,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "activity",
                        activityType = "steps",
                        newReadings = readings
                    )) {
                        syncCount++
                        Log.d(TAG, "Updated steps record with timestamp: ${record.startTime}, new count: ${record.count}")
                    }
                }
            }
            
            // Sync Distance
            val distanceRecords = healthConnectRepository.getDistance(from, to)
            for (record in distanceRecords) {
                val readings = listOf(
                    Reading("distance_meters", ValueType.DoubleValue(record.distance.inMeters)),
                    Reading("type", ValueType.StringValue("distance"))
                )
                
                if (!existingDistanceTimestamps.contains(record.startTime)) {
                    // Insert new record
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
                        Log.d(TAG, "Synced new distance record with timestamp: ${record.startTime}")
                    }
                } else {
                    // Update existing record
                    if (sensorReadingRepository.updateSensorReading(
                        userId = userId,
                        timestamp = record.startTime,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),

                        sensorType = "activity",
                        activityType = "distance",
                        newReadings = readings
                    )) {
                        syncCount++
                        Log.d(TAG, "Updated distance record with timestamp: ${record.startTime}, new distance: ${record.distance.inMeters}")
                    }
                }
            }
            
            // Sync Calories
            val caloriesRecords = healthConnectRepository.getTotalCaloriesBurned(from, to)
            for (record in caloriesRecords) {
                val readings = listOf(
                    Reading("calories", ValueType.DoubleValue(record.energy.inKilocalories)),
                    Reading("type", ValueType.StringValue("calories"))
                )
                
                if (!existingCaloriesTimestamps.contains(record.startTime)) {
                    // Insert new record
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
                        Log.d(TAG, "Synced new calories record with timestamp: ${record.startTime}")
                    }
                } else {
                    // Update existing record
                    if (sensorReadingRepository.updateSensorReading(
                        userId = userId,
                        timestamp = record.startTime,
                        deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                        sensorType = "activity",
                        activityType = "calories",
                        newReadings = readings
                    )) {
                        syncCount++
                        Log.d(TAG, "Updated calories record with timestamp: ${record.startTime}, new calories: ${record.energy.inKilocalories}")
                    }
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
            // Get existing sleep records from database to check for duplicates
            val existingSleepRecords = sensorReadingRepository.getSensorReadings(
                userId = userId,
                deviceId = null,
                from = from,
                to = to
            ).filter { sensorReading ->
                sensorReading.metadata.sensorType == "sleep" &&
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "sleep" }
            }
            
            // Extract existing start times for comparison
            val existingStartTimes = existingSleepRecords.mapNotNull { sensorReading ->
                sensorReading.readings.find { it.key == "start_time" }?.let { reading ->
                    (reading.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string?.let { 
                        try {
                            Instant.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }.toSet()
            
            val sleepRecords = healthConnectRepository.getSleepSessions(from, to)
            for (record in sleepRecords) {
                // Check if this record already exists by comparing startTime
                if (!existingStartTimes.contains(record.startTime)) {
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
                        Log.d(TAG, "Synced new sleep record with startTime: ${record.startTime}")
                    }
                } else {
                    Log.d(TAG, "Skipped duplicate sleep record with startTime: ${record.startTime}")
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
            // Get existing heart rate records from database to check for duplicates
            val existingHeartRateRecords = sensorReadingRepository.getSensorReadings(
                userId = userId,
                deviceId = null,
                from = from,
                to = to
            ).filter { sensorReading ->
                sensorReading.metadata.sensorType == "heart_rate" &&
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "heart_rate" }
            }
            
            // Extract existing timestamps for comparison
            val existingTimestamps = existingHeartRateRecords.mapNotNull { sensorReading ->
                sensorReading.readings.find { it.key == "time" }?.let { reading ->
                    (reading.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string?.let { 
                        try {
                            Instant.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }.toSet()
            
            val heartRateRecords = healthConnectRepository.getHeartRates(from, to)
            for (record in heartRateRecords) {
                for (sample in record.samples) {
                    // Check if this sample already exists by comparing timestamp
                    if (!existingTimestamps.contains(sample.time)) {
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
                            Log.d(TAG, "Synced new heart rate record with timestamp: ${sample.time}")
                        }
                    } else {
                        Log.d(TAG, "Skipped duplicate heart rate record with timestamp: ${sample.time}")
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
            // Get existing SpO2 records from database to check for duplicates
            val existingSpO2Records = sensorReadingRepository.getSensorReadings(
                userId = userId,
                deviceId = null,
                from = from,
                to = to
            ).filter { sensorReading ->
                sensorReading.metadata.sensorType == "spo2" &&
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "spo2" }
            }
            
            // Extract existing timestamps for comparison
            val existingTimestamps = existingSpO2Records.mapNotNull { sensorReading ->
                sensorReading.readings.find { it.key == "time" }?.let { reading ->
                    (reading.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string?.let { 
                        try {
                            Instant.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }.toSet()
            
            val spo2Records = healthConnectRepository.getOxygenSaturation(from, to)
            for (record in spo2Records) {
                // Check if this record already exists by comparing timestamp
                if (!existingTimestamps.contains(record.time)) {
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
                        Log.d(TAG, "Synced new SpO2 record with timestamp: ${record.time}")
                    }
                } else {
                    Log.d(TAG, "Skipped duplicate SpO2 record with timestamp: ${record.time}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync SpO2 data", e)
        }
        
        return syncCount
    }

    @SuppressLint("RestrictedApi")
    private suspend fun syncExerciseData(userId: ObjectId, from: Instant, to: Instant): Int {
        var syncCount = 0
        
        try {
            // Get existing exercise records from database to check for duplicates
            val existingExerciseRecords = sensorReadingRepository.getSensorReadings(
                userId = userId,
                deviceId = null,
                from = from,
                to = to
            ).filter { sensorReading ->
                sensorReading.metadata.sensorType == "exercise" &&
                sensorReading.readings.any { it.key == "type" && 
                    (it.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string == "exercise" }
            }
            
            // Extract existing start times for comparison
            val existingStartTimes = existingExerciseRecords.mapNotNull { sensorReading ->
                sensorReading.readings.find { it.key == "start_time" }?.let { reading ->
                    (reading.value as? com.example.mobile_health_app.data.model.ValueType.StringValue)?.string?.let { 
                        try {
                            Instant.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }.toSet()
            
            val exerciseRecords = healthConnectRepository.getExerciseSessions(from, to)
            for (record in exerciseRecords) {
                // Check if this record already exists by comparing startTime
                if (!existingStartTimes.contains(record.startTime)) {
                    val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                    val readings = listOf(
                        Reading("exercise_type", ValueType.StringValue(ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP[record.exerciseType] ?: "unknown")),
                        Reading("duration_minutes", ValueType.IntValue(durationMinutes.toInt())),
                        Reading("start_time", ValueType.StringValue(record.startTime.toString())),
                        Reading("end_time", ValueType.StringValue(record.endTime.toString())),
                        Reading("title", ValueType.StringValue(record.title ?: "")),
                        Reading("type", ValueType.StringValue("exercise"))
                    )
                    val sensorReading = SensorReading(
                        timestamp = record.startTime,
                        metadata = Metadata(
                            userId = userId,
                            deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID),
                            sensorType = "exercise"
                        ),
                        readings = readings
                    )
                    if (sensorReadingRepository.insertSensorReading(sensorReading)) {
                        syncCount++
                        Log.d(TAG, "Synced new exercise record with startTime: ${record.startTime}")
                    }
                } else {
                    Log.d(TAG, "Skipped duplicate exercise record with startTime: ${record.startTime}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync exercise data", e)
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