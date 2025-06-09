package com.example.mobile_health_app.data.shealth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import org.mongodb.kbson.ObjectId
import java.util.*

/**
 * Repository interface for health data operations
 */
interface HealthRepository {
    suspend fun initializeConnection()
    suspend fun disconnectFromHealth()
    fun isConnected(): Boolean
    fun getDailyStepCount(userId: ObjectId, date: Date): Flow<List<HealthStepData>>
    fun getHeartRateData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthHeartRateData>>
    fun getBloodPressureData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthBloodPressureData>>
    fun getWeightData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthWeightData>>
}

/**
 * Implementation of HealthRepository that works with Samsung Health
 */
class SamsungHealthRepository(private val context: Context) : HealthRepository {
    private val TAG = "SamsungHealthRepo"
    private val healthManager by lazy { HealthDataManager.getInstance(context) }
    
    override suspend fun initializeConnection() {
        healthManager.initialize()
    }
    
    override suspend fun disconnectFromHealth() {
        healthManager.disconnect()
    }
    
    override fun isConnected(): Boolean {
        return healthManager.isConnected
    }
    
    /**
     * Gets step count data for a specific day
     * @param userId User ID (for logging purposes)
     * @param date The date to get step data for
     * @return Flow of step count data
     */
    override fun getDailyStepCount(userId: ObjectId, date: Date): Flow<List<HealthStepData>> = flow {
        Log.d(TAG, "Getting step count for user $userId on date $date")
        
        // Set time to beginning of day
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.time
        
        // Set time to end of day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endTime = calendar.time
        
        val stepData = healthManager.readStepCount(startTime, endTime)
        emit(stepData)
    }.catch { e ->
        Log.e(TAG, "Error getting step count: ${e.message}")
        emit(emptyList())
    }
    
    /**
     * Gets heart rate data for a time range
     * @param userId User ID (for logging purposes)
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Flow of heart rate data
     */
    override fun getHeartRateData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthHeartRateData>> = flow {
        Log.d(TAG, "Getting heart rate data for user $userId from $startTime to $endTime")
        
        val heartRateData = healthManager.readHeartRate(startTime, endTime)
        emit(heartRateData)
    }.catch { e ->
        Log.e(TAG, "Error getting heart rate data: ${e.message}")
        emit(emptyList())
    }
    
    /**
     * Gets blood pressure data for a time range
     * @param userId User ID (for logging purposes)
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Flow of blood pressure data
     */
    override fun getBloodPressureData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthBloodPressureData>> = flow {
        Log.d(TAG, "Getting blood pressure data for user $userId from $startTime to $endTime")
        
        val bpData = healthManager.readBloodPressure(startTime, endTime)
        emit(bpData)
    }.catch { e ->
        Log.e(TAG, "Error getting blood pressure data: ${e.message}")
        emit(emptyList())
    }
    
    /**
     * Gets weight data for a time range
     * @param userId User ID (for logging purposes)
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Flow of weight data
     */
    override fun getWeightData(userId: ObjectId, startTime: Date, endTime: Date): Flow<List<HealthWeightData>> = flow {
        Log.d(TAG, "Getting weight data for user $userId from $startTime to $endTime")
        
        val weightData = healthManager.readWeight(startTime, endTime)
        emit(weightData)
    }.catch { e ->
        Log.e(TAG, "Error getting weight data: ${e.message}")
        emit(emptyList())
    }
}
