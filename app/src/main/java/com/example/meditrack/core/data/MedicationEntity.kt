package com.example.meditrack.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val amount: String = "",
    val unit: String = "mg",
    val pillsPerDose: Int = 1,
    val category: String = "MEDICINE",
    val reminderIntervalMinutes: Int = 5,
    val doseIntervalHours: Int = 0,
    val treatmentStartDate: Long = 0L,
    val treatmentDurationDays: Int = 0,
    val instructions: String = "",
    val timesCsv: String,
    val stock: Int,
    val lowStockThreshold: Int,
    val colorIndex: Int = 0,
    val active: Boolean = true,
    val deactivatedAt: Long = 0L,
    val trackingResumedAt: Long = 0L,
    val acknowledgedStockLevel: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val times: List<String>
        get() {
            val configured = timesCsv.split(",").map(String::trim).filter(String::isNotBlank).sorted()
            if (doseIntervalHours <= 0) return configured
            val start = configured.firstOrNull() ?: return emptyList()
            val parts = start.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return emptyList()
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return emptyList()
            if (hour !in 0..23 || minute !in 0..59 || 24 % doseIntervalHours != 0) return emptyList()
            val startMinutes = hour * 60 + minute
            return List(24 / doseIntervalHours) { index ->
                val total = (startMinutes + index * doseIntervalHours * 60) % (24 * 60)
                "%02d:%02d".format(total / 60, total % 60)
            }.sorted()
        }

    val scheduleDescription: String
        get() = if (doseIntervalHours > 0) {
            "Cada $doseIntervalHours h · inicia ${timesCsv.substringBefore(',')}"
        } else times.joinToString(" · ")

    val displayDose: String
        get() = if (pillsPerDose == 1) "1 pastilla" else "$pillsPerDose pastillas"

    val reminderIntervalMillis: Long
        get() = reminderIntervalMinutes.coerceAtLeast(1) * 60_000L

    val stockLevel: StockLevel
        get() = when {
            stock <= 0 -> StockLevel.OUT
            stock <= lowStockThreshold -> StockLevel.LOW
            stock <= lowStockThreshold * 2 -> StockLevel.MEDIUM
            else -> StockLevel.GOOD
        }

    val hasUnacknowledgedStockAlert: Boolean
        get() = active && stockLevel != StockLevel.GOOD && acknowledgedStockLevel != stockLevel.name

    val normalizedTreatmentStartDate: Long
        get() = startOfDay(treatmentStartDate.takeIf { it > 0 } ?: createdAt)

    val treatmentEndDate: Long?
        get() = if (treatmentDurationDays <= 0) null else Calendar.getInstance().apply {
            timeInMillis = normalizedTreatmentStartDate
            add(Calendar.DAY_OF_YEAR, treatmentDurationDays - 1)
        }.timeInMillis

    fun isTreatmentDay(time: Long): Boolean {
        val day = startOfDay(time)
        return day >= normalizedTreatmentStartDate &&
            (treatmentEndDate == null || day <= treatmentEndDate!!)
    }

    val finiteTreatmentPeriodLabel: String?
        get() = treatmentEndDate?.let { end ->
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "NI"))
            "${formatter.format(Date(normalizedTreatmentStartDate))} – ${formatter.format(Date(end))}"
        }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

enum class TreatmentCategory { MEDICINE, SUPPLEMENT }

enum class StockLevel { OUT, LOW, MEDIUM, GOOD }
