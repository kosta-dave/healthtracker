package com.kosta.healthtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosta.healthtracker.data.HealthRecord
import com.kosta.healthtracker.data.HealthRecordRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.sql.Date

class HealthRecordViewModel(private val repository: HealthRecordRepository) : ViewModel() {
    // Вот объявление allRecords
    val allRecords: Flow<List<HealthRecord>> = repository.allRecords

    fun insert(record: HealthRecord) = viewModelScope.launch {
        repository.insert(record)
    }

    fun update(record: HealthRecord) = viewModelScope.launch {
        repository.update(record)
    }

    fun delete(record: HealthRecord) = viewModelScope.launch {
        repository.delete(record)
    }

    suspend fun getRecordByDate(date: Date): HealthRecord? {
        return repository.getRecordByDate(date)
    }
}