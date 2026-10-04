package com.example.meditrack.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicationId", "scheduledAt"], unique = true)]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Long,
    val status: String,
    val actionAt: Long = System.currentTimeMillis()
)

enum class DoseStatus { PENDING, TAKEN, SKIPPED, POSTPONED }

data class DailyDose(
    val medication: MedicationEntity,
    val scheduledAt: Long,
    val time: String,
    val status: DoseStatus,
    val logId: Long? = null
)

data class HistoryItem(
    val log: DoseLogEntity,
    val medicationName: String,
    val dose: String
)
