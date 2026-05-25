package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DataUsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DataUsageDao {
    @Query("SELECT * FROM data_usage_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<DataUsageRecord>>

    @Query("SELECT * FROM data_usage_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDate(date: String): DataUsageRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: DataUsageRecord)

    @Query("SELECT * FROM data_usage_records WHERE date LIKE :monthPattern ORDER BY date DESC")
    suspend fun getRecordsForMonth(monthPattern: String): List<DataUsageRecord>

    @Query("SELECT * FROM data_usage_records WHERE date >= :startDate ORDER BY date DESC")
    fun getRecordsFromDateFlow(startDate: String): Flow<List<DataUsageRecord>>

    @Query("DELETE FROM data_usage_records")
    suspend fun clearAllRecords()
}
