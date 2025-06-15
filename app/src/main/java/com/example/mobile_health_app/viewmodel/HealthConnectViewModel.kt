package com.example.mobile_health_app.viewmodel

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobile_health_app.data.hconnect.HealthConnectRepository
import kotlinx.coroutines.launch
import java.time.Instant

import androidx.health.connect.client.records.*


class HealthConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthConnectRepository(application)

    private val _steps = MutableLiveData<List<StepsRecord>>()
    val steps: LiveData<List<StepsRecord>> = _steps

    private val _heartRates = MutableLiveData<List<HeartRateRecord>>()
    val heartRates: LiveData<List<HeartRateRecord>> = _heartRates

    private val _sleepSessions = MutableLiveData<List<SleepSessionRecord>>()
    val sleepSessions: LiveData<List<SleepSessionRecord>> = _sleepSessions

    private val _distance = MutableLiveData<List<DistanceRecord>>()
    val distance: LiveData<List<DistanceRecord>> = _distance

   private val _oxygenSaturation = MutableLiveData<List<OxygenSaturationRecord>>()
    val oxygenSaturation: LiveData<List<OxygenSaturationRecord>> = _oxygenSaturation

    private val _bloodPressure = MutableLiveData<List<BloodPressureRecord>>()
    val bloodPressure: LiveData<List<BloodPressureRecord>> = _bloodPressure

    private val _bodyTemperature = MutableLiveData<List<BodyTemperatureRecord>>()
    val bodyTemperature: LiveData<List<BodyTemperatureRecord>> = _bodyTemperature

    private val _totalCaloriesBurned = MutableLiveData<List<TotalCaloriesBurnedRecord>>()
    val totalCaloriesBurned: LiveData<List<TotalCaloriesBurnedRecord>> = _totalCaloriesBurned

    private val _restingHeartRate = MutableLiveData<List<RestingHeartRateRecord>>()
    val restingHeartRate: LiveData<List<RestingHeartRateRecord>> = _restingHeartRate

    private val _exerciseSessions = MutableLiveData<List<ExerciseSessionRecord>>()
    val exerciseSessions: LiveData<List<ExerciseSessionRecord>> = _exerciseSessions

    private val _floorsClimbed = MutableLiveData<List<FloorsClimbedRecord>>()
    val floorsClimbed: LiveData<List<FloorsClimbedRecord>> = _floorsClimbed


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
    /**
     * Tải dữ liệu ngủ từ [start] đến [end].
     */
    fun loadSleepSessions(start: Instant, end: Instant) {
        viewModelScope.launch {
            _sleepSessions.value = repository.getSleepSessions(start, end)
        }
    }
    /**
     * Tải  dữ liệu khoảng cách  từ [start] đến [end].
     */
    fun loadDistance(start: Instant, end: Instant) {
        viewModelScope.launch {
            _distance.value = repository.getDistance(start, end)
        }

    }
    /**
     * Tải dữ liệu getOxygenSaturation từ [start] đến [end].
     */

    fun loadOxygenSaturation(start: Instant, end: Instant) {
        viewModelScope.launch {
            _oxygenSaturation.value = repository.getOxygenSaturation(start, end)
        }

    }
    /**
     * Tải dữ liệu getBloodPressure từ [start] đến [end].
     */
    fun loadBloodPressure(start: Instant, end: Instant) {
        viewModelScope.launch {
            _bloodPressure.value = repository.getBloodPressure(start, end)
        }

    }
    /**
     * Tải dữ liệu getBodyTemperature từ [start] đến [end].
     */
    fun loadBodyTemperature(start: Instant, end: Instant) {
        viewModelScope.launch {
            _bodyTemperature.value = repository.getBodyTemperature(start, end)
        }

    }
    /**
     * Tải dữ liệu getTotalCaloriesBurned từ [start] đến [end].
     */
    fun loadTotalCaloriesBurned(start: Instant, end: Instant) {
        viewModelScope.launch {
            _totalCaloriesBurned.value = repository.getTotalCaloriesBurned(start, end)
        }
    }
    /**
     * Tải dữ liệu getRestingHeartRate từ [start] đến [end].
     */
    fun loadRestingHeartRate(start: Instant, end: Instant) {
        viewModelScope.launch {
            _restingHeartRate.value = repository.getRestingHeartRate(start, end)
        }
    }
    /**
     * Tải dữ liệu getExerciseSessions từ [start] đến [end].
     */
    fun loadExerciseSessions(start: Instant, end: Instant) {
        viewModelScope.launch {
            _exerciseSessions.value = repository.getExerciseSessions(start, end)
        }

    }
    /**
     * Tải tất cả dữ liệu getFloorsClimbed trong khoảng thời gian [start] đến [end].
     */
    fun loadFloorsClimbed(start: Instant, end: Instant) {
        viewModelScope.launch {
            _floorsClimbed.value = repository.getFloorsClimbed(start, end)
        }
    }
    /**
     * Tải tất cả dữ liệu từ Health Connect trong khoảng thời gian [start] đến [end].
     */
    fun loadAllHealthData(start: Instant, end: Instant) {
        loadSteps(start, end)
        loadHeartRates(start, end)
        loadSleepSessions(start, end)
        loadDistance(start, end)
        loadOxygenSaturation(start, end)
        loadBloodPressure(start, end)
        loadBodyTemperature(start, end)
        loadTotalCaloriesBurned(start, end)
        loadRestingHeartRate(start, end)
        loadExerciseSessions(start, end)
        loadFloorsClimbed(start, end)
    }
}