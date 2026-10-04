package com.example.meditrack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao
) {
    val medications: Flow<List<MedicationEntity>> = medicationDao.observeAll()
    val logs: Flow<List<DoseLogEntity>> = doseLogDao.observeAll()

    val todayDoses: Flow<List<DailyDose>> = combine(medications, logs) { medications, logs ->
        val today = startOfDay(System.currentTimeMillis())
        val tomorrow = today + DAY_MS
        val logsByKey = logs.filter { it.scheduledAt in today until tomorrow }
            .associateBy { it.medicationId to it.scheduledAt }

        medications.filter { it.active }.flatMap { medication ->
            medication.times.mapNotNull { time ->
                val scheduledAt = timeOnDay(today, time) ?: return@mapNotNull null
                val log = logsByKey[medication.id to scheduledAt]
                DailyDose(
                    medication = medication,
                    scheduledAt = scheduledAt,
                    time = time,
                    status = log?.status?.let { runCatching { DoseStatus.valueOf(it) }.getOrNull() }
                        ?: DoseStatus.PENDING,
                    logId = log?.id
                )
            }
        }.sortedBy { it.scheduledAt }
    }

    val history: Flow<List<HistoryItem>> = combine(medications, logs) { medications, logs ->
        val byId = medications.associateBy { it.id }
        logs.mapNotNull { log ->
            byId[log.medicationId]?.let { HistoryItem(log, it.name, it.displayDose) }
        }
    }

    suspend fun save(medication: MedicationEntity): Long =
        if (medication.id == 0L) medicationDao.insert(medication)
        else { medicationDao.update(medication); medication.id }

    suspend fun delete(medication: MedicationEntity) {
        doseLogDao.deleteForMedication(medication.id)
        medicationDao.delete(medication)
    }

    suspend fun getMedication(id: Long) = medicationDao.getById(id)
    suspend fun activeMedications() = medicationDao.getActive()
    suspend fun getDoseLog(medicationId: Long, scheduledAt: Long) =
        doseLogDao.getForDose(medicationId, scheduledAt)
    suspend fun refill(id: Long, amount: Int) = medicationDao.addStock(id, amount)

    suspend fun recordDose(dose: DailyDose, status: DoseStatus) {
        val previous = dose.status
        doseLogDao.insert(
            DoseLogEntity(
                id = dose.logId ?: 0,
                medicationId = dose.medication.id,
                scheduledAt = dose.scheduledAt,
                status = status.name
            )
        )
        if (status == DoseStatus.TAKEN && previous != DoseStatus.TAKEN) {
            medicationDao.consume(dose.medication.id, dose.medication.pillsPerDose)
        }
    }

    companion object {
        const val DAY_MS = 86_400_000L

        fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun timeOnDay(dayStart: Long, time: String): Long? {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return Calendar.getInstance().apply {
                timeInMillis = dayStart
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun formatDate(time: Long): String =
            SimpleDateFormat("dd MMM yyyy", Locale("es", "NI")).format(Date(time))
    }
}
