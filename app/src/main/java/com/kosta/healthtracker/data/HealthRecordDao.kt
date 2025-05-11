package com.kosta.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.sql.Date

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM health_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<HealthRecord>>

    @Insert
    suspend fun insert(record: HealthRecord)

    @Update
    suspend fun update(record: HealthRecord)

    @Delete
    suspend fun delete(record: HealthRecord)

    @Query("SELECT * FROM health_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: java.sql.Date): HealthRecord?
}