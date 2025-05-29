package com.example.mobile_health_app.data.model
import org.mongodb.kbson.*
import java.time.Instant


data class SensorReading(
    val _id: ObjectId = ObjectId(), // MongoDB sẽ tự sinh nếu không truyền vào
    val timestamp: Instant, // org.mongodb.kbson.Instant (hoặc java.time.Instant nếu bạn muốn)
    val metadata: Metadata,
    val readings: List<Reading>
)

data class Metadata(
    val userId: ObjectId,
    val deviceId: String,
    val sensorType: String? = null // Có thể null nếu không bắt buộc
)

data class Reading(
    val key: String,
    val value: ValueType
)

// Kotlin sealed class cho value có thể nhiều loại

sealed class ValueType {
    data class IntValue(val int: Int): ValueType()
    data class DoubleValue(val double: Double): ValueType()
    data class StringValue(val string: String): ValueType()
    data class BoolValue(val bool: Boolean): ValueType()
}
