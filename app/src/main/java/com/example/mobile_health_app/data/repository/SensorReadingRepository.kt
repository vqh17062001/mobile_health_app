package com.example.mobile_health_app.data.repository

import android.util.Log
import com.example.mobile_health_app.data.RealmConfig
import com.example.mobile_health_app.data.model.SensorReading
import com.example.mobile_health_app.data.model.Metadata
import com.example.mobile_health_app.data.model.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mongodb.kbson.*
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.ext.insertOne
import io.realm.kotlin.mongodb.ext.find
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalKBsonSerializerApi::class)
class SensorReadingRepository {
    private val TAG = "SensorReadingRepository"

    val mongoClient = "mongodb-atlas"
    val databaseName = "health_monitor"
    val collectionName = "sensor_readings"

    // Helper method to get current ISO timestamp (for fallback)
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return sdf.format(Date())
    }

    // Insert a new sensor reading
    suspend fun insertSensorReading(sensorReading: SensorReading): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val readings = db.collection(collectionName)

            val doc = BsonDocument().apply {
                put("timestamp", BsonDateTime(sensorReading.timestamp.epochSecond * 1000))
                put("metadata", BsonDocument().apply {
                    put("userId", sensorReading.metadata.userId)
                    put("deviceId", BsonString(sensorReading.metadata.deviceId))
                    sensorReading.metadata.sensorType?.let {
                        put("sensorType", BsonString(it))
                    }
                })
                put("readings", BsonArray().apply {
                    sensorReading.readings.forEach { r ->
                        add(BsonDocument().apply {
                            put("key", BsonString(r.key))
                            // Xử lý kiểu value tùy loại (int, double, string, bool)
                            when (val v = r.value) {
                                is com.example.mobile_health_app.data.model.ValueType.IntValue -> put("value", BsonInt32(v.int))
                                is com.example.mobile_health_app.data.model.ValueType.DoubleValue -> put("value", BsonDouble(v.double))
                                is com.example.mobile_health_app.data.model.ValueType.StringValue -> put("value", BsonString(v.string))
                                is com.example.mobile_health_app.data.model.ValueType.BoolValue -> put("value", BsonBoolean(v.bool))
                            }
                        })
                    }
                })
            }

            readings.insertOne(doc)
            Log.d(TAG, "SensorReading inserted successfully: $doc")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Insert failed: ${e.message}")
            return@withContext false
        }
    }

    // Lấy readings của 1 user (có thể lọc theo device hoặc time nếu muốn)
    /**
     * Lấy readings của 1 user, có thể lọc thêm theo deviceId và khoảng thời gian.
     * @param userId: ObjectId của user
     * @param deviceId: nếu null thì lấy tất cả thiết bị, không null thì filter đúng deviceId
     * @param from: thời gian bắt đầu (Instant), null = không giới hạn
     * @param to: thời gian kết thúc (Instant), null = không giới hạn
     */
    suspend fun getSensorReadings(
        userId: ObjectId,
        deviceId: String? = null,
        from: java.time.Instant? = null,
        to: java.time.Instant? = null
    ): List<SensorReading> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SensorReading>()
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val readings = db.collection(collectionName)

            // Tạo query động
            val query = BsonDocument().apply {
                // Lọc theo userId trong metadata
                put("metadata.userId", userId)
                // Nếu có deviceId thì filter thêm
                deviceId?.let {
                    put("metadata.deviceId", BsonString(it))
                }
                // Nếu có from hoặc to thì filter timestamp
                if (from != null || to != null) {
                    val timeCond = BsonDocument()
                    from?.let { timeCond.put("\$gte", BsonDateTime(it.toEpochMilli())) }
                    to?.let { timeCond.put("\$lte", BsonDateTime(it.toEpochMilli())) }
                    put("timestamp", timeCond)
                }
            }

            val docs = readings.find(query).toList()
            docs.forEach { doc ->
                try {
                    val timestamp = doc["timestamp"]?.asDateTime()?.value
                        ?: System.currentTimeMillis()
                    val meta = doc["metadata"]?.asDocument()
                    val _userId = meta?.get("userId") as? ObjectId ?: userId
                    val _deviceId = meta?.get("deviceId")?.asString()?.value ?: ""
                    val sensorType = meta?.get("sensorType")?.asString()?.value

                    val metadata = Metadata(
                        userId = _userId,
                        deviceId = _deviceId,
                        sensorType = sensorType
                    )

                    val readingsList = mutableListOf<Reading>()
                    val readingsArray = doc["readings"]?.asArray()
                    readingsArray?.values?.forEach { readingDoc ->
                        val reading = readingDoc.asDocument()
                        val key = reading["key"]?.asString()?.value ?: ""
                        val valueAny = reading["value"]
                        val value = when {
                            valueAny?.isInt32() == true -> com.example.mobile_health_app.data.model.ValueType.IntValue(valueAny.asInt32().value)
                            valueAny?.isDouble() == true -> com.example.mobile_health_app.data.model.ValueType.DoubleValue(valueAny.asDouble().value)
                            valueAny?.isString() == true -> com.example.mobile_health_app.data.model.ValueType.StringValue(valueAny.asString().value)
                            valueAny?.isBoolean() == true -> com.example.mobile_health_app.data.model.ValueType.BoolValue(valueAny.asBoolean().value)
                            else -> com.example.mobile_health_app.data.model.ValueType.StringValue("")
                        }
                        readingsList.add(Reading(key, value))
                    }

                    val sr = SensorReading(
                        timestamp = java.time.Instant.ofEpochMilli(timestamp),
                        metadata = metadata,
                        readings = readingsList
                    )
                    result.add(sr)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse document error: ${e.message}")
                }
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Get sensor readings failed: ${e.message}")
            return@withContext emptyList()
        }
    }

    // Lấy sensor reading mới nhất của user + device
    suspend fun getLatestSensorReading(userId: ObjectId, deviceId: String): SensorReading? = withContext(Dispatchers.IO) {
        try {
            val readings = getSensorReadings(userId).filter { it.metadata.deviceId == deviceId }
            return@withContext readings.maxByOrNull { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Get latest reading failed: ${e.message}")
            return@withContext null
        }
    }
}
