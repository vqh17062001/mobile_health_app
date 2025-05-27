package com.example.mobile_health_app.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import com.example.mobile_health_app.data.repository.UserRepository
import com.example.mobile_health_app.data.model.User

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializeRepository()
    }

    private fun initializeRepository() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.initialize()
            if (success) {
                // Listen for database changes
                repository.getAllUsers().collect { userList ->
                    _users.value = userList
                }
            } else {
                _errorMessage.value = "Không thể kết nối database"
            }
            _isLoading.value = false
        }
    }

    // Authentication methods
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.loginUser(username, password)
            if (user != null) {
                _currentUser.value = user
            } else {
                _errorMessage.value = "Đăng nhập không thành công"
            }
            _isLoading.value = false
        }
    }

    fun register(
        username: String,
        password: String,
        fullName: String,
        gender: String,
        dob: String,
        email: String,
        phone: String,
        role: String = "hocvien",
        department: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.registerUser(
                username, password, fullName, gender, dob,
                email, phone, role, department
            )
            if (!success) {
                _errorMessage.value = "Đăng ký không thành công"
            }
            _isLoading.value = false
        }
    }

    // User CRUD operations
    fun updateUser(id: ObjectId, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateUser(id, updates)
            if (!success) {
                _errorMessage.value = "Không thể cập nhật thông tin người dùng"
            }
            _isLoading.value = false
        }
    }

    fun changePassword(id: ObjectId, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.changePassword(id, oldPassword, newPassword)
            if (!success) {
                _errorMessage.value = "Không thể đổi mật khẩu"
            }
            _isLoading.value = false
        }
    }

    fun deleteUser(id: ObjectId) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteUser(id)
            if (!success) {
                _errorMessage.value = "Không thể xóa người dùng"
            }
            _isLoading.value = false
        }
    }

    fun deleteUserByUsername(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteUserByUsername(username)
            if (!success) {
                _errorMessage.value = "Không thể xóa người dùng"
            }
            _isLoading.value = false
        }
    }

    // User query methods
    fun getUserById(id: ObjectId) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.getUserById(id)
            if (user != null) {
                _currentUser.value = user
            } else {
                _errorMessage.value = "Không tìm thấy người dùng"
            }
            _isLoading.value = false
        }
    }

    fun getUserByUsername(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.getUserByUsername(username)
            if (user != null) {
                _currentUser.value = user
            } else {
                _errorMessage.value = "Không tìm thấy người dùng"
            }
            _isLoading.value = false
        }
    }

    fun getUserByEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.getUserByEmail(email)
            if (user != null) {
                _currentUser.value = user
            } else {
                _errorMessage.value = "Không tìm thấy người dùng"
            }
            _isLoading.value = false
        }
    }

    // User list query methods
    fun searchUsersByFullName(fullName: String) {
        viewModelScope.launch {
            repository.getUsersByFullName(fullName).collect { userList ->
                _users.value = userList
            }
        }
    }

    fun getUsersByRole(role: String) {
        viewModelScope.launch {
            repository.getUsersByRole(role).collect { userList ->
                _users.value = userList
            }
        }
    }

    fun getUsersByDepartment(department: String) {
        viewModelScope.launch {
            repository.getUsersByDepartment(department).collect { userList ->
                _users.value = userList
            }
        }
    }

    fun getAllUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collect { userList ->
                _users.value = userList
            }
        }
    }

    fun deleteAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteAllUsers()
            if (!success) {
                _errorMessage.value = "Không thể xóa tất cả người dùng"
            }
            _isLoading.value = false
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Clear current user (logout)
    fun clearCurrentUser() {
        _currentUser.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}