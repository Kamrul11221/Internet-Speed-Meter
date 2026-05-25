package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_usage_records")
data class DataUsageRecord(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val wifiRx: Long = 0L,        // wifi downloaded bytes
    val wifiTx: Long = 0L,        // wifi uploaded bytes
    val mobileRx: Long = 0L,      // mobile downloaded bytes
    val mobileTx: Long = 0L       // mobile uploaded bytes
) {
    val totalWifi: Long get() = wifiRx + wifiTx
    val totalMobile: Long get() = mobileRx + mobileTx
    val totalRx: Long get() = wifiRx + mobileRx
    val totalTx: Long get() = wifiTx + mobileTx
    val totalBytes: Long get() = wifiRx + wifiTx + mobileRx + mobileTx
}
