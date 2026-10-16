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

enum class DoseStatus { PENDING, TAKEN, SKIPPED, POSTPONED, MISSED }

data class DailyDose(
    val medication: MedicationEntity,
    val scheduledAt: Long,
    val time: String,
    val status: DoseStatus,
    val logId: Long? = null,
    val actionAt: Long? = null
) {
    val availableAt: Long
        get() = if (status == DoseStatus.POSTPONED && actionAt != null) {
            actionAt + medication.reminderIntervalMillis
        } else {
            scheduledAt
        }

    fun canBeTakenAt(now: Long): Boolean =
        (status == DoseStatus.PENDING || status == DoseStatus.POSTPONED) &&
            medication.stock > 0 && now >= availableAt && !isExpiredAt(now)

    fun isExpiredAt(now: Long): Boolean = now >= scheduledAt + MedicationRepository.DAY_MS
}

data class HistoryItem(
    val log: DoseLogEntity,
    val medicationName: String,
    val dose: String
)

data class StockOutageHistoryItem(
    val outage: StockOutageEntity,
    val medicationName: String
)
