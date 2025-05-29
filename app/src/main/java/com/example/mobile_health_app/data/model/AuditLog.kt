package com.example.mobile_health_app.data.model


import org.mongodb.kbson.ObjectId


data class AuditLog(
    val _id: ObjectId = ObjectId(),
    val eventAt: String = "",     // ISODate (dạng chuỗi ISO 8601)
    val userId: ObjectId,
    val action: String = "",      // Tên hành động (ví dụ: "login", "add_device", "logout", ...)
    val resource: String = "",    // Tên loại tài nguyên bị tác động (vd: "user", "device", "sensor_reading", ...)
    val resourceId: ObjectId? = null, // id tài nguyên bị tác động (có thể null nếu không liên quan resource nào)
    val ipAddress: String = "",
    val detail: Map<String, Any?> = emptyMap() // Lưu thông tin bổ sung, tùy ý (có thể là json, object...)
)
