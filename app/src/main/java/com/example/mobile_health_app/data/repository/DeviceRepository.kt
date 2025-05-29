package com.example.mobile_health_app.data.repository

import android.util.Log
import com.example.mobile_health_app.data.RealmConfig
import com.example.mobile_health_app.data.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mongodb.kbson.*
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.ext.insertOne
import io.realm.kotlin.mongodb.ext.findOne
import io.realm.kotlin.mongodb.ext.find
import io.realm.kotlin.mongodb.ext.updateOne

@OptIn(ExperimentalKBsonSerializerApi::class)
class DeviceRepository {
    private val TAG = "DeviceRepository"

    val mongoClient = "mongodb-atlas"
    val databaseName = "health_monitor"
    val collectionName = "devices"

    // Thêm thiết bị mới
    suspend fun insertDevice(device: Device): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val devices = db.collection(collectionName)

            val doc = BsonDocument().apply {
                put("deviceId", BsonString(device.deviceId))
                put("ownerId", device.ownerId)
                put("model", BsonString(device.model))
                put("osVersion", BsonString(device.osVersion))
                put("sdkVersion", BsonString(device.sdkVersion))
                put("registeredAt", BsonString(device.registeredAt))
                put("lastSyncAt", BsonString(device.lastSyncAt))
                put("status", BsonString(device.status))
            }

            devices.insertOne(doc)
            Log.d(TAG, "Device inserted: ${device.deviceId}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Insert device failed: ${e.message}")
            return@withContext false
        }
    }

    // Lấy tất cả thiết bị của 1 user
    suspend fun getDevicesByOwner(ownerId: ObjectId): List<Device> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Device>()
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val devices = db.collection(collectionName)

            val query = BsonDocument("ownerId", ownerId)
            val docs = devices.find(query).toList()
            docs.forEach { doc ->
                try {
                    val _deviceId = doc["deviceId"]?.asString()?.value ?: ""
                    val _ownerId = doc["ownerId"] as? ObjectId ?: ownerId
                    val _model = doc["model"]?.asString()?.value ?: ""
                    val _osVersion = doc["osVersion"]?.asString()?.value ?: ""
                    val _sdkVersion = doc["sdkVersion"]?.asString()?.value ?: ""
                    val _registeredAt = doc["registeredAt"]?.asString()?.value ?: ""
                    val _lastSyncAt = doc["lastSyncAt"]?.asString()?.value ?: ""
                    val _status = doc["status"]?.asString()?.value ?: ""

                    val device = Device(
                        deviceId = _deviceId,
                        ownerId = _ownerId,
                        model = _model,
                        osVersion = _osVersion,
                        sdkVersion = _sdkVersion,
                        registeredAt = _registeredAt,
                        lastSyncAt = _lastSyncAt,
                        status = _status
                    )
                    result.add(device)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse device failed: ${e.message}")
                }
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Get devices failed: ${e.message}")
            return@withContext emptyList()
        }
    }

    // Lấy device theo deviceId
    suspend fun getDeviceById(deviceId: String): Device? = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val devices = db.collection(collectionName)

            val query = BsonDocument("deviceId", BsonString(deviceId))
            val doc = devices.findOne(query)
            if (doc != null) {
                return@withContext Device(
                    deviceId = doc["deviceId"]?.asString()?.value ?: "",
                    ownerId = doc["ownerId"] as? ObjectId ?: ObjectId(),
                    model = doc["model"]?.asString()?.value ?: "",
                    osVersion = doc["osVersion"]?.asString()?.value ?: "",
                    sdkVersion = doc["sdkVersion"]?.asString()?.value ?: "",
                    registeredAt = doc["registeredAt"]?.asString()?.value ?: "",
                    lastSyncAt = doc["lastSyncAt"]?.asString()?.value ?: "",
                    status = doc["status"]?.asString()?.value ?: ""
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Get device by id failed: ${e.message}")
            return@withContext null
        }
    }

    // Cập nhật trạng thái online/offline (có thể mở rộng thêm các cập nhật khác)
    suspend fun updateDeviceStatus(deviceId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val devices = db.collection(collectionName)

            val query = BsonDocument("deviceId", BsonString(deviceId))
            val update = BsonDocument("\$set", BsonDocument("status", BsonString(status)))

            val result = devices.updateOne(query, update)
            Log.d(TAG, "Update device $deviceId status: $status")
            return@withContext result.updated
        } catch (e: Exception) {
            Log.e(TAG, "Update device status failed: ${e.message}")
            return@withContext false
        }
    }
}
