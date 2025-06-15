package com.example.mobile_health_app.data.hconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*



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
//
        // Heart rate
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
//
        // Exercise
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
//
//
        // Sleep
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),

        // Blood Pressure
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(BloodPressureRecord::class),


        // Hydration
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
//
        // Nutrition
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),

        // Distance
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
//
        // Calories Burned
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
//
        // Resting Heart Rate
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getWritePermission(RestingHeartRateRecord::class),

        // Cycling Cadence
        HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
        HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class),

        // Elevation
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getWritePermission(ElevationGainedRecord::class),
//
        // Floors Climbed
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getWritePermission(FloorsClimbedRecord::class),

        // Power
        HealthPermission.getReadPermission(PowerRecord::class),
        HealthPermission.getWritePermission(PowerRecord::class),

        // Skin Temperature
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
        HealthPermission.getWritePermission(SkinTemperatureRecord::class),
//
        // Speed
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),

        // Steps Cadence
        HealthPermission.getReadPermission(StepsCadenceRecord::class),
        HealthPermission.getWritePermission(StepsCadenceRecord::class),

        // Wheelchair Pushes
        HealthPermission.getReadPermission(WheelchairPushesRecord::class),
        HealthPermission.getWritePermission(WheelchairPushesRecord::class)
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
