package com.example.mobile_health_app.data.hconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.RestingHeartRateRecord

import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectRepository(context: Context) {
    private val client: HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    /**
     * Đọc tất cả StepsRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getSteps(start: Instant, end: Instant): List<StepsRecord> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả HeartRateRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getHeartRates(start: Instant, end: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả ExerciseSessionRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả SleepSessionRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getSleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả WeightRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getWeightRecords(start: Instant, end: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả BloodPressureRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getBloodPressure(start: Instant, end: Instant): List<BloodPressureRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả BodyTemperatureRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getBodyTemperature(start: Instant, end: Instant): List<BodyTemperatureRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả OxygenSaturationRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getOxygenSaturation(start: Instant, end: Instant): List<OxygenSaturationRecord> {
        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả BasalMetabolicRateRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getBasalMetabolicRate(
        start: Instant,
        end: Instant
    ): List<BasalMetabolicRateRecord> {
        val request = ReadRecordsRequest(
            recordType = BasalMetabolicRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả HydrationRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getHydration(start: Instant, end: Instant): List<HydrationRecord> {
        val request = ReadRecordsRequest(
            recordType = HydrationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả NutritionRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getNutrition(start: Instant, end: Instant): List<NutritionRecord> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả DistanceRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getDistance(start: Instant, end: Instant): List<DistanceRecord> {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả TotalCaloriesBurnedRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getTotalCaloriesBurned(
        start: Instant,
        end: Instant
    ): List<TotalCaloriesBurnedRecord> {
        val request = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả RestingHeartRateRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getRestingHeartRate(start: Instant, end: Instant): List<RestingHeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = RestingHeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }

    /**
     * Đọc tất cả FloorsClimbedRecord từ khoảng thời gian [start] đến [end].
     */
    suspend fun getFloorsClimbed(start: Instant, end: Instant): List<FloorsClimbedRecord> {
        val request = ReadRecordsRequest(
            recordType = FloorsClimbedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return client.readRecords(request).records
    }
}
