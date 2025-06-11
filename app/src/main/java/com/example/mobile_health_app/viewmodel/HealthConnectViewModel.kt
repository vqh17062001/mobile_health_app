package com.example.mobile_health_app.viewmodel

import android.app.Application
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.hconnect.HealthConnectRepository
import kotlinx.coroutines.launch
import java.time.Instant

class HealthConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthConnectRepository(application)

    private val _steps = MutableLiveData<List<StepsRecord>>()
    val steps: LiveData<List<StepsRecord>> = _steps

    private val _heartRates = MutableLiveData<List<HeartRateRecord>>()
    val heartRates: LiveData<List<HeartRateRecord>> = _heartRates

    /**
     * Tải dữ liệu bước chân từ [start] đến [end].
     */
    fun loadSteps(start: Instant, end: Instant) {
        viewModelScope.launch {
            _steps.value = repository.getSteps(start, end)
        }
    }

    /**
     * Tải dữ liệu nhịp tim từ [start] đến [end].
     */
    fun loadHeartRates(start: Instant, end: Instant) {
        viewModelScope.launch {
            _heartRates.value = repository.getHeartRates(start, end)
        }
    }
}