package com.example.mobile_health_app.data.shealth

/**
 * Constants for Samsung Health integration
 * Use this class to centralize the health data type constants
 */
object HealthConstants {
    // Samsung Health data types
    const val STEP_COUNT_TYPE = "com.samsung.health.step_count"
    const val HEART_RATE_TYPE = "com.samsung.health.heart_rate"
    const val BLOOD_PRESSURE_TYPE = "com.samsung.health.blood_pressure"
    const val WEIGHT_TYPE = "com.samsung.health.weight"
    
    // Common field names
    const val START_TIME = "start_time"
    const val TIME_OFFSET = "time_offset"
    
    // Step count fields
    object StepCount {
        const val COUNT = "count"
        const val DISTANCE = "distance"
        const val CALORIE = "calorie"
        const val SPEED = "speed"
    }
    
    // Heart rate fields
    object HeartRate {
        const val HEART_RATE = "heart_rate"
    }
    
    // Blood pressure fields
    object BloodPressure {
        const val SYSTOLIC = "systolic"
        const val DIASTOLIC = "diastolic"
        const val PULSE = "pulse"
    }
    
    // Weight fields
    object Weight {
        const val WEIGHT = "weight"
    }
}
