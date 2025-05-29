package com.example.mobile_health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.model.AuditLog
import com.example.mobile_health_app.data.repository.AuditLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

class AuditLogViewModel : ViewModel() {
    private val repo = AuditLogRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _logs = MutableStateFlow<List<AuditLog>>(emptyList())
    val logs: StateFlow<List<AuditLog>> = _logs.asStateFlow()

    private val _insertSuccess = MutableStateFlow<Boolean>(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess.asStateFlow()

    // Ghi log mới
    fun insertLog(auditLog: AuditLog) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repo.insertAuditLog(auditLog)
                _insertSuccess.value = result
                if (!result) {
                    _errorMessage.value = "Không ghi được log"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi ghi log: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Lấy logs theo user (và có thể theo action)
    fun fetchLogsByUser(userId: ObjectId, action: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repo.getLogsByUser(userId, action)
                _logs.value = list
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi lấy log: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetInsertSuccess() {
        _insertSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
