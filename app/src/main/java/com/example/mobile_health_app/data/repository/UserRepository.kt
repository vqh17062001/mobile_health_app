package com.example.mobile_health_app.data.repository

import android.util.Log
import com.example.mobile_health_app.data.RealmConfig
import com.example.mobile_health_app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
//import org.bson.types.ObjectId
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.ext.insertOne
import io.realm.kotlin.mongodb.ext.findOne
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.mongodb.kbson.ExperimentalKBsonSerializerApi
import java.security.MessageDigest
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.*




@OptIn(ExperimentalKBsonSerializerApi::class)
class UserRepository {
    private val TAG = "UserRepository"
    

    val mongoClient = "mongodb-atlas"
    val databaseName = "health_monitor"
    val collectionName = "users"

    // Helper method to hash passwords
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    // Helper method to get current ISO timestamp
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return sdf.format(Date())
    }

    // Check if username already exists
    suspend fun checkUsernameExists(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val users = db.collection(collectionName)

            val query = BsonDocument("username", BsonString(username))

            val user = users.findOne(query)
            
            return@withContext user != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking username: ${e.message}")
            return@withContext false
        }
    }

    // Register new user with complete information
    suspend fun registerUser(
        username: String,
        password: String,
        fullName: String,
        gender: String,
        dob: String,
        email: String,
        phone: String,
        role: String = "hocvien",
        department: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if username already exists
            if (checkUsernameExists(username)) {
                Log.w(TAG, "Username already exists: $username")
                return@withContext false
            }
            
            val timestamp = getCurrentTimestamp()
            val hashedPassword = hashPassword(password)
            
            val user = User(
                _id = ObjectId(),
                username = username,
                passwordHash = hashedPassword,
                fullName = fullName,
                gender = gender,
                Dob = dob,
                role = role,
                department = department,
                email = email,
                phone = phone,

                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            insertUser(user)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            return@withContext false
        }
    }

    suspend fun insertUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient("mongodb-atlas")
            val db = mgcli.database("health_monitor")
            val users = db.collection("users")
            val timestamp = getCurrentTimestamp()
            val doc = BsonDocument().apply {
                                   // ObjectId
                put("username", BsonString(user.username))
                put("passwordHash", BsonString(user.passwordHash))
                put("fullName", BsonString(user.fullName))
                put("gender", BsonString(user.gender))
                put("Dob", BsonString(user.Dob))                  // Date → millis
                put("role", BsonString(user.role))
                put("department", BsonString(user.department))
                put("email", BsonString(user.email))
                put("phone", BsonString(user.phone))
                put("managerIds", BsonString(user.managerIds) ) // List<ObjectId>
                put("createdAt", BsonString(timestamp))
                put("updatedAt", BsonString(timestamp))
            }


            users.insertOne(doc)
            Log.d(TAG, "User inserted successfully: ${user.username}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Insert failed: ${e.message}")
            return@withContext false
        }
    }

    // Method to authenticate user (for future use)
    suspend fun loginUser(username: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val app = RealmConfig.app
            val userAuth = app.login(Credentials.anonymous())
            val mgcli = userAuth.mongoClient(mongoClient)
            val db = mgcli.database(databaseName)
            val users = db.collection(collectionName)
            
            val hashedPassword = hashPassword(password)
            val query = BsonDocument().apply {
                put("username", BsonString(username))
                put("passwordHash",BsonString(hashedPassword))
            }
            
            val doc = users.findOne(query)
            if (doc != null) {

                // Convert Document to User
                return@withContext User(
                    _id = doc["_id"]!!.asObjectId(),
                    username = doc["username"] .toString(),
                    passwordHash = doc["passwordHash"] .toString(),
                    fullName = doc["fullName"] .toString(),
                    gender = doc["gender"] .toString(),
                    Dob = doc["Dob"] .toString(),
                    role = doc["role"] .toString(),
                    department = doc["department"] .toString(),
                    email = doc["email"] .toString(),
                    phone = doc["phone"] .toString(),
                    createdAt = doc["createdAt"] .toString(),
                    updatedAt = doc["updatedAt"].toString()
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            return@withContext null
        }
    }
}