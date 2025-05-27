package com.example.mobile_health_app.data.repository

import com.example.mobile_health_app.data.RealmConfig
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.User as RealmUser
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import com.example.mobile_health_app.data.model.*
import java.text.SimpleDateFormat
import java.util.Date

class UserRepository {
    private val app = RealmConfig.app
    private lateinit var realm: Realm
    private var currentUser: RealmUser? = null

    // Khởi tạo kết nối
    suspend fun initialize(): Boolean {
        return try {
            // Đăng nhập anonymous (hoặc sử dụng phương thức xác thực khác)
            currentUser = app.login(Credentials.anonymous())

            // Cấu hình Realm với sync
            val config = SyncConfiguration.Builder(
                currentUser!!,
                setOf(User::class)
            ).build()

            realm = Realm.open(config)
            true
        } catch (e: Exception) {
            println("Lỗi khởi tạo: ${e.message}")
            false
        }
    }

    // Đăng nhập người dùng bằng username và password
    suspend fun loginUser(username: String, password: String): User? {
        return try {
            // Trong thực tế, bạn cần mã hóa password và so sánh với passwordHash
            // Đây chỉ là ví dụ đơn giản
            val hashedPassword = hashPassword(password) // Implement hashing function
            realm.query<User>("username == $0 AND passwordHash == $1", 
                username, hashedPassword).first().find()
        } catch (e: Exception) {
            println("Lỗi đăng nhập: ${e.message}")
            null
        }
    }

    // Hàm mã hóa password (cần triển khai theo thuật toán an toàn)
    private fun hashPassword(password: String): String {
        // Implement a secure password hashing algorithm
        // For example, use BCrypt or PBKDF2
        return password // placeholder, replace with actual hashing
    }

    // CREATE - Đăng ký người dùng mới
    suspend fun registerUser(
        username: String, 
        password: String,
        fullName: String,
        gender: String,
        dob: String,
        email: String,
        phone: String,
        role: String = "hocvien", // Default role
        department: String = ""
    ): Boolean {
        return try {
            // Kiểm tra username đã tồn tại chưa
            val existingUser = realm.query<User>("username == $0", username).first().find()
            if (existingUser != null) {
                println("Username đã tồn tại")
                return false
            }

            val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(Date())

            realm.write {
                val newUser = User().apply {
                    this.username = username
                    this.passwordHash = hashPassword(password)
                    this.fullName = fullName
                    this.gender = gender
                    this.Dob = dob
                    this.role = role
                    this.department = department
                    this.email = email
                    this.phone = phone
                    this.managerIds = "" // Placeholder, should be handled better
                    this.createdAt = currentTime
                    this.updatedAt = currentTime
                }
                copyToRealm(newUser)
            }
            true
        } catch (e: Exception) {
            println("Lỗi tạo user: ${e.message}")
            false
        }
    }

    // READ - Lấy tất cả users (Real-time)
    fun getAllUsers(): Flow<List<User>> {
        return realm.query<User>()
            .asFlow()
            .map { results -> results.list.toList() }
    }

    // READ - Lấy user theo ID
    suspend fun getUserById(id: ObjectId): User? {
        return try {
            realm.query<User>("_id == $0", id).first().find()
        } catch (e: Exception) {
            println("Lỗi lấy user theo ID: ${e.message}")
            null
        }
    }

    // READ - Tìm kiếm user theo username
    suspend fun getUserByUsername(username: String): User? {
        return try {
            realm.query<User>("username == $0", username).first().find()
        } catch (e: Exception) {
            println("Lỗi lấy user theo username: ${e.message}")
            null
        }
    }

    // READ - Tìm kiếm user theo tên đầy đủ
    fun getUsersByFullName(fullName: String): Flow<List<User>> {
        return realm.query<User>("fullName CONTAINS[c] $0", fullName)
            .asFlow()
            .map { results -> results.list.toList() }
    }

    // READ - Lấy users theo vai trò
    fun getUsersByRole(role: String): Flow<List<User>> {
        return realm.query<User>("role == $0", role)
            .asFlow()
            .map { results -> results.list.toList() }
    }

    // READ - Lấy users theo phòng ban
    fun getUsersByDepartment(department: String): Flow<List<User>> {
        return realm.query<User>("department == $0", department)
            .asFlow()
            .map { results -> results.list.toList() }
    }

    // READ - Lấy user theo email
    suspend fun getUserByEmail(email: String): User? {
        return try {
            realm.query<User>("email == $0", email).first().find()
        } catch (e: Exception) {
            println("Lỗi lấy user theo email: ${e.message}")
            null
        }
    }

    // UPDATE - Cập nhật thông tin người dùng
    suspend fun updateUser(id: ObjectId, updates: Map<String, Any>): Boolean {
        return try {
            realm.write {
                val user = query<User>("_id == $0", id).first().find()
                user?.let { foundUser ->
                    updates.forEach { (key, value) ->
                        when (key) {
                            "username" -> foundUser.username = value as String
                            "passwordHash" -> foundUser.passwordHash = value as String
                            "fullName" -> foundUser.fullName = value as String
                            "gender" -> foundUser.gender = value as String
                            "Dob" -> foundUser.Dob = value as String
                            "role" -> foundUser.role = value as String
                            "department" -> foundUser.department = value as String
                            "email" -> foundUser.email = value as String
                            "phone" -> foundUser.phone = value as String
                            "managerIds" -> foundUser.managerIds = value as String
                        }
                    }
                    
                    // Update the updatedAt timestamp
                    foundUser.updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .format(Date())
                }
            }
            true
        } catch (e: Exception) {
            println("Lỗi cập nhật user: ${e.message}")
            false
        }
    }

    // UPDATE - Đổi mật khẩu người dùng
    suspend fun changePassword(id: ObjectId, oldPassword: String, newPassword: String): Boolean {
        return try {
            val user = getUserById(id)
            if (user != null && user.passwordHash == hashPassword(oldPassword)) {
                realm.write {
                    val realmUser = query<User>("_id == $0", id).first().find()
                    realmUser?.let {
                        it.passwordHash = hashPassword(newPassword)
                        it.updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(Date())
                    }
                }
                true
            } else {
                println("Mật khẩu cũ không đúng")
                false
            }
        } catch (e: Exception) {
            println("Lỗi đổi mật khẩu: ${e.message}")
            false
        }
    }

    // DELETE - Xóa user theo ID
    suspend fun deleteUser(id: ObjectId): Boolean {
        return try {
            realm.write {
                val user = query<User>("_id == $0", id).first().find()
                user?.let { delete(it) }
            }
            true
        } catch (e: Exception) {
            println("Lỗi xóa user: ${e.message}")
            false
        }
    }

    // DELETE - Xóa user theo username
    suspend fun deleteUserByUsername(username: String): Boolean {
        return try {
            realm.write {
                val user = query<User>("username == $0", username).first().find()
                user?.let { delete(it) }
            }
            true
        } catch (e: Exception) {
            println("Lỗi xóa user theo username: ${e.message}")
            false
        }
    }

    // DELETE - Xóa tất cả users
    suspend fun deleteAllUsers(): Boolean {
        return try {
            realm.write {
                val users = query<User>().find()
                delete(users)
            }
            true
        } catch (e: Exception) {
            println("Lỗi xóa tất cả users: ${e.message}")
            false
        }
    }

    // Đóng kết nối
    fun close() {
        if (::realm.isInitialized) {
            realm.close()
        }
    }
}