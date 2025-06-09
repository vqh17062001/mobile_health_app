package com.example.mobile_health_app.data.shealth

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.healthdata.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.samsung.android.sdk.healthdata.HealthConstants


/**
 * Class that manages connection and operations with Samsung Health SDK
 */
class HealthDataManager private constructor(private val context: Context) {
    
    private val TAG = "HealthDataManager"
    
    // Samsung Health Data Store
    private var mStore: HealthDataStore? = null
    
    // Connection listener to handle Samsung Health connection events
    private val mConnectionListener = object : HealthDataStore.ConnectionListener {
        override fun onConnected() {
            Log.d(TAG, "Connected to Samsung Health")
            isConnected = true
        }

        override fun onConnectionFailed(error: HealthConnectionErrorResult) {
            Log.e(TAG, "Connection to Samsung Health failed: ${error.errorCode}")
            isConnected = false
        }

        override fun onDisconnected() {
            Log.d(TAG, "Disconnected from Samsung Health")
            isConnected = false
        }
    }
    
    // Connection state
    var isConnected = false
        private set
    
    /**
     * Initializes the connection to Samsung Health
     */
    fun initialize() {
        Log.d(TAG, "Initializing Samsung Health connection")
        try {
            mStore = HealthDataStore(context, mConnectionListener)
            mStore?.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Samsung Health: ${e.message}")
        }
    }
    
    /**
     * Disconnects from Samsung Health
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from Samsung Health")
        mStore?.disconnectService()
        mStore = null
        isConnected = false
    }
    
    /**
     * Suspending function to get step count data for a specific day
     * @param startTime Beginning of time range
     * @param endTime End of time range
     * @return List of step count data points
     */
    suspend fun readStepCount(startTime: Date, endTime: Date): List<HealthStepData> = 
        withContext(Dispatchers.IO) {
            if (!isConnected || mStore == null) {
                throw IllegalStateException("Not connected to Samsung Health")
            }
            
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, startTime.time),
                HealthDataResolver.Filter.lessThanEquals(HealthConstants.StepCount.START_TIME, endTime.time)
            )
            
            val resolver = HealthDataResolver(mStore, null)
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.StepCount.COUNT,
                    HealthConstants.StepCount.DISTANCE,
                    HealthConstants.StepCount.CALORIE,
                    HealthConstants.StepCount.SPEED,
                    HealthConstants.StepCount.START_TIME,
                    HealthConstants.StepCount.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()
            
            return@withContext suspendCancellableCoroutine { continuation ->
                try {
                    val result = mutableListOf<HealthStepData>()
                    
                    resolver.read(request).setResultListener { response ->
                        try {
                            val iterator = response.iterator()
                            while (iterator.hasNext()) {
                                val data = iterator.next()
                                
                                val stepData = HealthStepData(
                                    count = data.getInt(HealthConstants.StepCount.COUNT),
                                    distance = data.getFloat(HealthConstants.StepCount.DISTANCE),
                                    calorie = data.getFloat(HealthConstants.StepCount.CALORIE),
                                    speed = data.getFloat(HealthConstants.StepCount.SPEED),
                                    startTime = Date(data.getLong(HealthConstants.StepCount.START_TIME)),
                                    timeOffset = data.getInt(HealthConstants.StepCount.TIME_OFFSET)
                                )
                                
                                result.add(stepData)
                            }
                            
                            continuation.resume(result)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading step count data: ${e.message}")
                            continuation.resumeWithException(e)
                        } finally {
                            try {
                                response.close() // Use response.close() instead of iterator().close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error closing response: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read step count data: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    
    /**
     * Reads heart rate data for a specific time range
     * @param startTime Beginning of time range
     * @param endTime End of time range
     * @return List of heart rate data points
     */
    suspend fun readHeartRate(startTime: Date, endTime: Date): List<HealthHeartRateData> = 
        withContext(Dispatchers.IO) {
            if (!isConnected || mStore == null) {
                throw IllegalStateException("Not connected to Samsung Health")
            }
            
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime.time),
                HealthDataResolver.Filter.lessThanEquals("start_time", endTime.time)
            )
            
            val resolver = HealthDataResolver(mStore, null)
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.HeartRate.HEART_RATE,
                    HealthConstants.HeartRate.START_TIME,
                    HealthConstants.HeartRate.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()
            
            return@withContext suspendCancellableCoroutine { continuation ->
                try {
                    val result = mutableListOf<HealthHeartRateData>()
                    
                    resolver.read(request).setResultListener { response ->
                        try {
                            val iterator = response.iterator()
                            while (iterator.hasNext()) {
                                val data = iterator.next()
                                
                                val heartRateData = HealthHeartRateData(
                                    heartRate = data.getInt(HealthConstants.HeartRate.HEART_RATE),
                                    startTime = Date(data.getLong(HealthConstants.HeartRate.START_TIME)),
                                    timeOffset = data.getInt(HealthConstants.HeartRate.TIME_OFFSET)
                                )
                                
                                result.add(heartRateData)
                            }
                            
                            continuation.resume(result)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading heart rate data: ${e.message}")
                            continuation.resumeWithException(e)
                        } finally {
                            try {
                                response.close() // Use response.close() instead of iterator().close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error closing response: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read heart rate data: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    
    /**
     * Reads blood pressure data for a specific time range
     * @param startTime Beginning of time range
     * @param endTime End of time range
     * @return List of blood pressure data points
     */
    suspend fun readBloodPressure(startTime: Date, endTime: Date): List<HealthBloodPressureData> = 
        withContext(Dispatchers.IO) {
            if (!isConnected || mStore == null) {
                throw IllegalStateException("Not connected to Samsung Health")
            }
            
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime.time),
                HealthDataResolver.Filter.lessThanEquals("start_time", endTime.time)
            )
            
            val resolver = HealthDataResolver(mStore, null)
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.BloodPressure.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.BloodPressure.SYSTOLIC,
                    HealthConstants.BloodPressure.DIASTOLIC,
                    HealthConstants.BloodPressure.PULSE,
                    HealthConstants.BloodPressure.START_TIME,
                    HealthConstants.BloodPressure.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()
            
            return@withContext suspendCancellableCoroutine { continuation ->
                try {
                    val result = mutableListOf<HealthBloodPressureData>()
                    
                    resolver.read(request).setResultListener { response ->
                        try {
                            val iterator = response.iterator()
                            while (iterator.hasNext()) {
                                val data = iterator.next()
                                
                                val bpData = HealthBloodPressureData(
                                    systolic = data.getInt(HealthConstants.BloodPressure.SYSTOLIC),
                                    diastolic = data.getInt(HealthConstants.BloodPressure.DIASTOLIC),
                                    pulse = data.getInt(HealthConstants.BloodPressure.PULSE),
                                    startTime = Date(data.getLong(HealthConstants.BloodPressure.START_TIME)),
                                    timeOffset = data.getInt(HealthConstants.BloodPressure.TIME_OFFSET)
                                )
                                
                                result.add(bpData)
                            }
                            
                            continuation.resume(result)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading blood pressure data: ${e.message}")
                            continuation.resumeWithException(e)
                        } finally {
                            try {
                                response.close() // Use response.close() instead of iterator().close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error closing response: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read blood pressure data: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    
    /**
     * Reads weight data for a specific time range
     * @param startTime Beginning of time range
     * @param endTime End of time range
     * @return List of weight data points
     */
    suspend fun readWeight(startTime: Date, endTime: Date): List<HealthWeightData> = 
        withContext(Dispatchers.IO) {
            if (!isConnected || mStore == null) {
                throw IllegalStateException("Not connected to Samsung Health")
            }
            
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime.time),
                HealthDataResolver.Filter.lessThanEquals("start_time", endTime.time)
            )
            
            val resolver = HealthDataResolver(mStore, null)
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Weight.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Weight.WEIGHT,
                    HealthConstants.Weight.START_TIME,
                    HealthConstants.Weight.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()
            
            return@withContext suspendCancellableCoroutine { continuation ->
                try {
                    val result = mutableListOf<HealthWeightData>()
                    
                    resolver.read(request).setResultListener { response ->
                        try {
                            val iterator = response.iterator()
                            while (iterator.hasNext()) {
                                val data = iterator.next()
                                
                                val weightData = HealthWeightData(
                                    weight = data.getFloat(HealthConstants.Weight.WEIGHT),
                                    startTime = Date(data.getLong(HealthConstants.Weight.START_TIME)),
                                    timeOffset = data.getInt(HealthConstants.Weight.TIME_OFFSET)
                                )
                                
                                result.add(weightData)
                            }
                            
                            continuation.resume(result)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading weight data: ${e.message}")
                            continuation.resumeWithException(e)
                        } finally {
                            try {
                                response.close() // Use response.close() instead of iterator().close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error closing response: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read weight data: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    
    companion object {
        @Volatile
        private var INSTANCE: HealthDataManager? = null
        
        fun getInstance(context: Context): HealthDataManager {
            return INSTANCE ?: synchronized(this) {
                val instance = HealthDataManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

// Data classes to represent health data
data class HealthStepData(
    val count: Int,
    val distance: Float,
    val calorie: Float,
    val speed: Float,
    val startTime: Date,
    val timeOffset: Int
)

data class HealthHeartRateData(
    val heartRate: Int,
    val startTime: Date,
    val timeOffset: Int
)

data class HealthBloodPressureData(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val startTime: Date,
    val timeOffset: Int
)

data class HealthWeightData(
    val weight: Float,
    val startTime: Date,
    val timeOffset: Int
)
