package com.example.mobile_health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.model.SensorReading
import com.example.mobile_health_app.data.repository.SensorReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import java.time.Instant

class SensorReadingViewModel : ViewModel() {
    private val repo = SensorReadingRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _readings = MutableStateFlow<List<SensorReading>>(emptyList())
    val readings: StateFlow<List<SensorReading>> = _readings.asStateFlow()

    private val _insertSuccess = MutableStateFlow<Boolean>(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess.asStateFlow()

    private val _latestReading = MutableStateFlow<SensorReading?>(null)
    val latestReading: StateFlow<SensorReading?> = _latestReading.asStateFlow()

    // Lấy readings của user, có thể filter device và time
    fun fetchReadings(
        userId: ObjectId,
        deviceId: String? = null,
        from: Instant? = null,
        to: Instant? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repo.getSensorReadings(userId, deviceId, from, to)
                _readings.value = data
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi lấy dữ liệu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Thêm một sensor reading mới
    fun insertSensorReading(sensorReading: SensorReading) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repo.insertSensorReading(sensorReading)
                _insertSuccess.value = result
                if (result) {
                    // Refresh list sau khi thêm
                    fetchReadings(sensorReading.metadata.userId)
                } else {
                    _errorMessage.value = "Không thể thêm dữ liệu cảm biến"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi thêm dữ liệu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Lấy sensor reading mới nhất theo user + device
    fun fetchLatestReading(userId: ObjectId, deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val latest = repo.getLatestSensorReading(userId, deviceId)
                _latestReading.value = latest
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi lấy dữ liệu mới nhất: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reset trạng thái thêm dữ liệu thành công
    fun resetInsertSuccess() {
        _insertSuccess.value = false
    }

    // Clear error
    fun clearError() {
        _errorMessage.value = null
    }
}
