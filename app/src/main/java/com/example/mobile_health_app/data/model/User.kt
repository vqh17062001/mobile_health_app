package com.example.mobile_health_app.data.model


import org.bson.types.ObjectId
import kotlinx.serialization.Serializable

//Serializable
data class User(
    val _id: ObjectId = ObjectId(),
    val username: String = "",
    val passwordHash: String = "",
    val fullName: String = "",
    val gender: String = "",
    val Dob: String = "", // ISODate dạng string ISO 8601
    val role: String = "",
    val department: String = "",
    val email: String = "",
    val phone: String = "",
    val managerIds:String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

/*
    "name": "users",
      "schema": {
        "_id": "ObjectId",
        "username": "String (unique)",
        "passwordHash": "String",
        "fullName": "String",

	"gender": "String",
	"Dob": "ISODate",
        "role": "String (hocvien|capquanly)",
        "department": "String",
        "email": "String (encrypted)",
        "phone": "String (encrypted)",
        "managerIds": ["ObjectId"],
        "createdAt": "ISODate",
        "updatedAt": "ISODate"
 */