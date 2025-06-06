package com.example.mobile_health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.model.User
import com.example.mobile_health_app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import org.mongodb.kbson.ObjectId

class UserViewModel : ViewModel() {
    private val repo = UserRepository()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // New StateFlow for password change success status
    private val _passwordChangeSuccessful = MutableStateFlow(false)
    val passwordChangeSuccessful: StateFlow<Boolean> = _passwordChangeSuccessful.asStateFlow()

    // Add new user directly (basic method)
    fun addNewUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repo.insertUser(user)
                if (success) {
                    _registrationSuccess.value = true
                } else {
                    _errorMessage.value = "Failed to register user"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Enhanced registration method with complete details
    fun registerUser(
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
            try {
                // Check if username exists
                if (repo.checkUsernameExists(username)) {
                    _errorMessage.value = "Username already exists. Please choose another."
                    _isLoading.value = false
                    return@launch
                }
                
                val success = repo.registerUser(
                    username, password, fullName, gender, dob,
                    email, phone, role, department
                )
                
                if (success) {
                    _registrationSuccess.value = true
                } else {
                    _errorMessage.value = "Registration failed. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Login method for future use
    fun loginUser(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repo.loginUser(username, password)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    _errorMessage.value = "Invalid username or password"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Load user by ID
    fun loadUserById(userId: ObjectId) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repo.getUserById(userId)
                _currentUser.value = user
                if (user == null) {
                    _errorMessage.value = "Không tìm thấy thông tin người dùng"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi tải dữ liệu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update user profile
    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repo.updateUser(user)
                if (success) {
                    _currentUser.value = user
                    _errorMessage.value = "Cập nhật thành công!"
                } else {
                    _errorMessage.value = "Cập nhật thất bại!"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi cập nhật: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // New method for changing password
    fun changePassword(userId: ObjectId, currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repo.changeUserPassword(userId, currentPassword, newPassword)
                if (success) {
                    _passwordChangeSuccessful.value = true
                } else {
                    _errorMessage.value = "Mật khẩu hiện tại không đúng"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi đổi mật khẩu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // New method specifically for updating managerIds
    fun updateUserManagerIds(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repo.updateUserManagerIds(user)
                if (success) {
                    // Update current user in memory
                    _currentUser.value = user
                    _errorMessage.value = "Cập nhật người quản lý thành công!"
                } else {
                    _errorMessage.value = "Cập nhật người quản lý thất bại!"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi cập nhật: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Reset password change status
    fun resetPasswordChangeStatus() {
        _passwordChangeSuccessful.value = false
    }
    
    // Reset state
    fun resetRegistrationState() {
        _registrationSuccess.value = false
    }
    
    // Clear error
    fun clearError() {
        _errorMessage.value = null
    }
}