package com.example.mobile_health_app.data.repository

import android.util.Log
import com.example.mobile_health_app.data.RealmConfig
import com.example.mobile_health_app.data.model.AuditLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mongodb.kbson.*
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.ext.insertOne
import io.realm.kotlin.mongodb.ext.find
import org.mongodb.kbson.BsonDocument

@OptIn(ExperimentalKBsonSerializerApi::class)
class AuditLogRepository {
    private val TAG = "AuditLogRepository"

    val mongoClient = "mongodb-atlas"
    val databaseName = "health_monitor"
    val collectionName = "audit_logs"

    // Thêm một bản ghi audit log mới
    suspend fun insertAuditLog(auditLog: AuditLog): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val logs = db.collection(collectionName)

            val doc = BsonDocument().apply {
                put("eventAt", BsonString(auditLog.eventAt))
                put("userId", auditLog.userId)
                put("action", BsonString(auditLog.action))
                put("resource", BsonString(auditLog.resource))
                auditLog.resourceId?.let { put("resourceId", it) }
                put("ipAddress", BsonString(auditLog.ipAddress))
                // Convert Map<String, Any?> sang BsonDocument cho detail
                val detailDoc = BsonDocument()
                auditLog.detail.forEach { (k, v) ->
                    when (v) {
                        is String -> detailDoc[k] = BsonString(v)
                        is Int -> detailDoc[k] = BsonInt32(v)
                        is Double -> detailDoc[k] = BsonDouble(v)
                        is Boolean -> detailDoc[k] = BsonBoolean(v)
                        is ObjectId -> detailDoc[k] = v
                        null -> detailDoc[k] = BsonNull.VALUE
                        else -> detailDoc[k] = BsonString(v.toString())
                    }
                }
                put("detail", detailDoc)
            }
            logs.insertOne(doc)
            Log.d(TAG, "Audit log inserted: ${auditLog.action}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Insert audit log failed: ${e.message}")
            return@withContext false
        }
    }

    // Lấy logs theo userId, có thể lọc theo action hoặc thời gian
    suspend fun getLogsByUser(
        userId: ObjectId,
        action: String? = null,
        from: String? = null,
        to: String? = null
    ): List<AuditLog> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AuditLog>()
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val logs = db.collection(collectionName)

            val query = BsonDocument("userId", userId)
            action?.let { query["action"] = BsonString(it) }
            // Nếu cần, có thể thêm filter thời gian với eventAt (dạng chuỗi ISO)
            // Bỏ qua filter thời gian để code gọn, bạn muốn thì mình viết luôn

            val docs = logs.find(query).toList()
            docs.forEach { doc ->
                try {
                    val _eventAt = doc["eventAt"]?.asString()?.value ?: ""
                    val _userId = doc["userId"] as? ObjectId ?: userId
                    val _action = doc["action"]?.asString()?.value ?: ""
                    val _resource = doc["resource"]?.asString()?.value ?: ""
                    val _resourceId = doc["resourceId"] as? ObjectId
                    val _ipAddress = doc["ipAddress"]?.asString()?.value ?: ""
                    val _detail = mutableMapOf<String, Any?>()
                    doc["detail"]?.asDocument()?.forEach { k, v ->
                        _detail[k] = when {
                            v.isInt32() -> v.asInt32().value
                            v.isDouble() -> v.asDouble().value
                            v.isString() -> v.asString().value
                            v.isBoolean() -> v.asBoolean().value
                            v.isObjectId() -> v.asObjectId()
                            v.isNull() -> null
                            else -> v.toString()
                        }
                    }
                    result.add(
                        AuditLog(
                            eventAt = _eventAt,
                            userId = _userId,
                            action = _action,
                            resource = _resource,
                            resourceId = _resourceId,
                            ipAddress = _ipAddress,
                            detail = _detail
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Parse log failed: ${e.message}")
                }
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Get logs failed: ${e.message}")
            return@withContext emptyList()
        }
    }
}
