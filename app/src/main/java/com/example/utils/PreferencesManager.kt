package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "internet_speed_meter_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_MOBILE_LIMIT_GB = "mobile_limit_gb"
        const val KEY_WIFI_LIMIT_GB = "wifi_limit_gb"
        const val KEY_ALERT_THRESHOLD_PCT = "alert_threshold_pct"
        const val KEY_AUTO_START = "auto_start_meter"
        const val KEY_ALERT_DISMISSED_MONTH = "alert_dismissed_month" // e.g. "2026-05"
        const val KEY_SPEED_UNIT = "speed_unit"
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_HIDE_NOTIFICATION = "hide_notification"

        // Default constraints
        const val DEFAULT_MOBILE_LIMIT_GB = 10f
        const val DEFAULT_WIFI_LIMIT_GB = 100f
        const val DEFAULT_ALERT_THRESHOLD_PCT = 90
        const val UNIT_BYTES = "BYTES"
        const val UNIT_BITS = "BITS"
    }

    var isServiceEnabled: Boolean // Tracks explicit user intent
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var isNotificationHidden: Boolean
        get() = prefs.getBoolean(KEY_HIDE_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_NOTIFICATION, value).apply()

    var speedUnit: String
        get() = prefs.getString(KEY_SPEED_UNIT, UNIT_BYTES) ?: UNIT_BYTES
        set(value) = prefs.edit().putString(KEY_SPEED_UNIT, value).apply()

    var mobileLimitGb: Float
        get() = prefs.getFloat(KEY_MOBILE_LIMIT_GB, DEFAULT_MOBILE_LIMIT_GB)
        set(value) = prefs.edit().putFloat(KEY_MOBILE_LIMIT_GB, value).apply()

    var wifiLimitGb: Float
        get() = prefs.getFloat(KEY_WIFI_LIMIT_GB, DEFAULT_WIFI_LIMIT_GB)
        set(value) = prefs.edit().putFloat(KEY_WIFI_LIMIT_GB, value).apply()

    var alertThresholdPct: Int
        get() = prefs.getInt(KEY_ALERT_THRESHOLD_PCT, DEFAULT_ALERT_THRESHOLD_PCT)
        set(value) = prefs.edit().putInt(KEY_ALERT_THRESHOLD_PCT, value).apply()

    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var dismissedAlertMonth: String?
        get() = prefs.getString(KEY_ALERT_DISMISSED_MONTH, null)
        set(value) = prefs.edit().putString(KEY_ALERT_DISMISSED_MONTH, value).apply()

    fun getMobileLimitBytes(): Long = (mobileLimitGb * 1024L * 1024L * 1024L).toLong()
    fun getWifiLimitBytes(): Long = (wifiLimitGb * 1024L * 1024L * 1024L).toLong()
}
