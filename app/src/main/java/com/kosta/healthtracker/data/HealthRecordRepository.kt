package com.kosta.healthtracker.data
import kotlinx.coroutines.flow.first
import java.sql.Date

class HealthRecordRepository(private val dao: HealthRecordDao) {
    val allRecords = dao.getAllRecords()

    suspend fun insert(record: HealthRecord) = dao.insert(record)
    suspend fun update(record: HealthRecord) = dao.update(record)
    suspend fun delete(record: HealthRecord) = dao.delete(record)

    suspend fun getRecordByDate(date: Date): HealthRecord? {
        return dao.getRecordByDate(date)
    }

    suspend fun getAllRecordsForExport(): List<HealthRecord> {
        return dao.getAllRecords().first()
    }
    suspend fun importRecords(records: List<HealthRecord>) {
        records.forEach { dao.insert(it) }
    }
}