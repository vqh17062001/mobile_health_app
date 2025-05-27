package com.example.mobile_health_app.data.model
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey


import org.mongodb.kbson.ObjectId

class User : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var username: String = ""
    var passwordHash: String = ""
    var fullName: String = ""
    var gender: String = ""
    var Dob: String = ""
    var role: String = ""
    var department: String = ""
    var email: String = ""
    var phone: String = ""
    var managerIds: String = ""
    var createdAt: String = ""
    var updatedAt: String = ""
}

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