package com.kosta.healthtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kosta.healthtracker.data.HealthRecordRepository

class HealthRecordViewModelFactory(
    private val repository: HealthRecordRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthRecordViewModel::class.java)) {
            return HealthRecordViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}