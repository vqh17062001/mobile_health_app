package com.example.mobile_health_app.data.model
import org.mongodb.kbson.ObjectId


data class Device(
    val _id: ObjectId = ObjectId(),
    val deviceId: String = "",
    val ownerId: ObjectId,
    val model: String = "",
    val osVersion: String = "",
    val sdkVersion: String = "",
    val registeredAt: String = "", // ISODate (ISO 8601 string, ví dụ: "2024-05-30T14:22:16.000Z")
    val lastSyncAt: String = "",   // ISODate
    val status: String = ""        // "online" hoặc "offline"
)
