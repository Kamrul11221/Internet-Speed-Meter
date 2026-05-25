package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpeedStateHolder {
    private val _downloadRate = MutableStateFlow(0L) // in bytes per second
    val downloadRate: StateFlow<Long> = _downloadRate.asStateFlow()

    private val _uploadRate = MutableStateFlow(0L) // in bytes per second
    val uploadRate: StateFlow<Long> = _uploadRate.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _todayWifiRx = MutableStateFlow(0L)
    val todayWifiRx = _todayWifiRx.asStateFlow()

    private val _todayWifiTx = MutableStateFlow(0L)
    val todayWifiTx = _todayWifiTx.asStateFlow()

    private val _todayMobileRx = MutableStateFlow(0L)
    val todayMobileRx = _todayMobileRx.asStateFlow()

    private val _todayMobileTx = MutableStateFlow(0L)
    val todayMobileTx = _todayMobileTx.asStateFlow()

    fun updateRates(download: Long, upload: Long) {
        _downloadRate.value = download
        _uploadRate.value = upload
    }

    fun updateServiceStatus(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateTodayStats(wifiRx: Long, wifiTx: Long, mobileRx: Long, mobileTx: Long) {
        _todayWifiRx.value = wifiRx
        _todayWifiTx.value = wifiTx
        _todayMobileRx.value = mobileRx
        _todayMobileTx.value = mobileTx
    }
}
