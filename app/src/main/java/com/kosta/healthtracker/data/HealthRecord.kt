package com.kosta.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var date: Date = Date(),
    var glucose: Float? = null,
    var ketones: Float? = null,
    var steps: Int? = null,
    var breakfastTime: String? = null,
    var lunchTime: String? = null,
    var dinnerTime: String? = null,
    var comment: String? = null,
    var waistSize: Float? = null,
    var dataVersion: Int = 1
)