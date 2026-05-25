package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SpeedStateHolder
import com.example.data.db.AppDatabase
import com.example.data.model.DataUsageRecord
import com.example.data.repository.DataUsageRepository
import com.example.service.SpeedMeterService
import com.example.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository: DataUsageRepository = DataUsageRepository(AppDatabase.getDatabase(application).dataUsageDao)
    private val prefs = PreferencesManager(context)

    // Dynamic states
    private val _mobileLimitGb = MutableStateFlow(prefs.mobileLimitGb)
    val mobileLimitGb: StateFlow<Float> = _mobileLimitGb.asStateFlow()

    private val _wifiLimitGb = MutableStateFlow(prefs.wifiLimitGb)
    val wifiLimitGb: StateFlow<Float> = _wifiLimitGb.asStateFlow()

    private val _alertThresholdPct = MutableStateFlow(prefs.alertThresholdPct)
    val alertThresholdPct: StateFlow<Int> = _alertThresholdPct.asStateFlow()

    private val _isAutoStartEnabled = MutableStateFlow(prefs.isAutoStartEnabled)
    val isAutoStartEnabled: StateFlow<Boolean> = _isAutoStartEnabled.asStateFlow()

    private val _isNotificationHidden = MutableStateFlow(prefs.isNotificationHidden)
    val isNotificationHidden: StateFlow<Boolean> = _isNotificationHidden.asStateFlow()

    private val _speedUnit = MutableStateFlow(prefs.speedUnit)
    val speedUnit: StateFlow<String> = _speedUnit.asStateFlow()

    val downloadRate = SpeedStateHolder.downloadRate
    val uploadRate = SpeedStateHolder.uploadRate
    val isServiceRunning = SpeedStateHolder.isServiceRunning

    // Today's real-time values from SpeedStateHolder
    val todayWifiRx = SpeedStateHolder.todayWifiRx
    val todayWifiTx = SpeedStateHolder.todayWifiTx
    val todayMobileRx = SpeedStateHolder.todayMobileRx
    val todayMobileTx = SpeedStateHolder.todayMobileTx

    // Flow of all database usage historical rows
    val usageHistory: StateFlow<List<DataUsageRecord>> = repository.allRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Month's breakdowns (computed reactively from history map)
    val monthlyMobileTotalBytes: StateFlow<Long> = usageHistory.map { records ->
        val currentMonthPattern = getCurrentMonthPrefix()
        records.filter { it.date.startsWith(currentMonthPattern) }
            .sumOf { it.totalMobile }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyWifiTotalBytes: StateFlow<Long> = usageHistory.map { records ->
        val currentMonthPattern = getCurrentMonthPrefix()
        records.filter { it.date.startsWith(currentMonthPattern) }
            .sumOf { it.totalWifi }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        // Launch service if it was left enabled
        if (prefs.isServiceEnabled && !isServiceRunning.value) {
            val intent = Intent(context, SpeedMeterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun toggleService(enable: Boolean) {
        prefs.isServiceEnabled = enable
        val intent = Intent(context, SpeedMeterService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    fun updateMobileLimit(limitGb: Float) {
        prefs.mobileLimitGb = limitGb
        _mobileLimitGb.value = limitGb
    }

    fun updateWifiLimit(limitGb: Float) {
        prefs.wifiLimitGb = limitGb
        _wifiLimitGb.value = limitGb
    }

    fun updateThreshold(pct: Int) {
        prefs.alertThresholdPct = pct
        _alertThresholdPct.value = pct
    }

    fun toggleAutoStart(enabled: Boolean) {
        prefs.isAutoStartEnabled = enabled
        _isAutoStartEnabled.value = enabled
    }

    fun toggleHideNotification(hidden: Boolean) {
        prefs.isNotificationHidden = hidden
        _isNotificationHidden.value = hidden
        if (isServiceRunning.value) {
            toggleService(true)
        }
    }

    fun updateSpeedUnit(unit: String) {
        prefs.speedUnit = unit
        _speedUnit.value = unit
        if (isServiceRunning.value) {
            toggleService(true)
        }
    }

    fun clearUsageHistory() {
        viewModelScope.launch {
            repository.clearAllRecords()
            SpeedStateHolder.updateTodayStats(0, 0, 0, 0)
        }
    }

    private fun getCurrentMonthPrefix(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }
}
