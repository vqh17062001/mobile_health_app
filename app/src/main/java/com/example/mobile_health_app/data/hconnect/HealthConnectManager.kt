package com.example.mobile_health_app.data.hconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

class HealthConnectManager(context: Context) {

    // 1. Khởi tạo HealthConnectClient khi cần, dùng lazy để chỉ tạo một lần
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    // 2. Tập hợp permissions giống hệt sample
    val permissions = setOf(
        // Steps
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),

        // Weight
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),

        // Heart rate
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),


    )

    // 3. Hàm check coi đã grant đủ hay chưa
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient
            .permissionController
            .getGrantedPermissions()
            .containsAll(permissions)
    }

    // 4. Contract để request permissions
    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }
}
