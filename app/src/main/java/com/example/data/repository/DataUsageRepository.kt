package com.example.data.repository

import com.example.data.dao.DataUsageDao
import com.example.data.model.DataUsageRecord
import kotlinx.coroutines.flow.Flow

class DataUsageRepository(private val dataUsageDao: DataUsageDao) {

    val allRecordsFlow: Flow<List<DataUsageRecord>> = dataUsageDao.getAllRecordsFlow()

    fun getRecordsFromDateFlow(startDate: String): Flow<List<DataUsageRecord>> =
        dataUsageDao.getRecordsFromDateFlow(startDate)

    suspend fun getRecordForDate(date: String): DataUsageRecord? {
        return dataUsageDao.getRecordForDate(date)
    }

    suspend fun addUsage(
        date: String,
        deltaWifiRx: Long,
        deltaWifiTx: Long,
        deltaMobileRx: Long,
        deltaMobileTx: Long
    ) {
        val existing = dataUsageDao.getRecordForDate(date)
        val updated = if (existing != null) {
            existing.copy(
                wifiRx = existing.wifiRx + deltaWifiRx,
                wifiTx = existing.wifiTx + deltaWifiTx,
                mobileRx = existing.mobileRx + deltaMobileRx,
                mobileTx = existing.mobileTx + deltaMobileTx
            )
        } else {
            DataUsageRecord(
                date = date,
                wifiRx = deltaWifiRx,
                wifiTx = deltaWifiTx,
                mobileRx = deltaMobileRx,
                mobileTx = deltaMobileTx
            )
        }
        dataUsageDao.insertOrUpdateRecord(updated)
    }

    suspend fun getRecordsForMonth(monthPattern: String): List<DataUsageRecord> {
        return dataUsageDao.getRecordsForMonth(monthPattern)
    }

    suspend fun clearAllRecords() {
        dataUsageDao.clearAllRecords()
    }
}
