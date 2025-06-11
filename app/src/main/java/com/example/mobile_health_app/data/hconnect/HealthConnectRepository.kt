package com.example.mobile_health_app.data.hconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
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
}
