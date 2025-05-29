package com.example.mobile_health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.model.Device
import com.example.mobile_health_app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

class DeviceViewModel : ViewModel() {
    private val repo = DeviceRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _insertSuccess = MutableStateFlow<Boolean>(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess.asStateFlow()

    private val _currentDevice = MutableStateFlow<Device?>(null)
    val currentDevice: StateFlow<Device?> = _currentDevice.asStateFlow()

    // Thêm device mới
    fun insertDevice(device: Device) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repo.insertDevice(device)
                _insertSuccess.value = result
                if (result) {
                    fetchDevicesByOwner(device.ownerId)
                } else {
                    _errorMessage.value = "Không thể thêm thiết bị"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi thêm thiết bị: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Lấy danh sách thiết bị của user
    fun fetchDevicesByOwner(ownerId: ObjectId) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repo.getDevicesByOwner(ownerId)
                _devices.value = list
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi lấy thiết bị: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Lấy device theo deviceId
    fun fetchDeviceById(deviceId: String, ownerId: ObjectId? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val device = repo.getDeviceById(deviceId, ownerId)
                _currentDevice.value = device
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi lấy thông tin thiết bị: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Cập nhật trạng thái thiết bị (online/offline)
    fun updateDeviceStatus(deviceId: String, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repo.updateDeviceStatus(deviceId, status)
                if (result) {
                    fetchDeviceById(deviceId)
                } else {
                    _errorMessage.value = "Không cập nhật được trạng thái"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi cập nhật trạng thái: ${e.message}"
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
