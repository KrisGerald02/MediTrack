package com.example.meditrack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val stockOutageDao: StockOutageDao
) {
    val medications: Flow<List<MedicationEntity>> = medicationDao.observeAll()
    val logs: Flow<List<DoseLogEntity>> = doseLogDao.observeAll()
    private val stockOutages: Flow<List<StockOutageEntity>> = stockOutageDao.observeAll()

    val todayDoses: Flow<List<DailyDose>> = combine(medications, logs) { medications, logs ->
        val now = System.currentTimeMillis()
        val today = startOfDay(now)
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val tomorrow = today + DAY_MS
        val logsByKey = logs.filter { it.scheduledAt in yesterday until tomorrow }
            .associateBy { it.medicationId to it.scheduledAt }

        medications.filter { it.active }.flatMap { medication ->
            listOf(yesterday, today).filter(medication::isTreatmentDay).flatMap { day ->
                medication.times.mapNotNull { time ->
                    val scheduledAt = timeOnDay(day, time) ?: return@mapNotNull null
                    val log = logsByKey[medication.id to scheduledAt]
                    DailyDose(
                        medication = medication,
                        scheduledAt = scheduledAt,
                        time = time,
                        status = log?.status?.let { runCatching { DoseStatus.valueOf(it) }.getOrNull() }
                            ?: DoseStatus.PENDING,
                        logId = log?.id,
                        actionAt = log?.actionAt
                    )
                }
            }
        }.filter { dose ->
            when {
                dose.status == DoseStatus.MISSED -> false
                dose.scheduledAt >= today -> true
                else -> dose.status == DoseStatus.PENDING || dose.status == DoseStatus.POSTPONED
            }
        }.sortedBy { it.scheduledAt }
    }

    val history: Flow<List<HistoryItem>> = combine(medications, logs) { medications, logs ->
        val byId = medications.associateBy { it.id }
        logs.mapNotNull { log ->
            byId[log.medicationId]?.let { HistoryItem(log, it.name, it.displayDose) }
        }
    }

    val outageHistory: Flow<List<StockOutageHistoryItem>> =
        combine(medications, stockOutages) { medications, outages ->
            val byId = medications.associateBy { it.id }
            outages.mapNotNull { outage ->
                byId[outage.medicationId]?.let { StockOutageHistoryItem(outage, it.name) }
            }
        }

    suspend fun save(medication: MedicationEntity): Long {
        val now = System.currentTimeMillis()
        val previous = medication.id.takeIf { it != 0L }?.let { medicationDao.getById(it) }
        val normalized = medication.copy(
            deactivatedAt = when {
                medication.active -> 0L
                previous?.active == false && previous.deactivatedAt > 0 -> previous.deactivatedAt
                else -> now
            },
            trackingResumedAt = when {
                previous?.active == false && medication.active -> startOfDay(now)
                previous != null -> previous.trackingResumedAt
                else -> 0L
            },
            acknowledgedStockLevel = if (previous?.stock == medication.stock) {
                previous.acknowledgedStockLevel
            } else ""
        )
        val id = if (normalized.id == 0L) medicationDao.insert(normalized)
        else {
            medicationDao.update(normalized)
            normalized.id
        }
        updateOutageForStockChange(id, previous?.stock, normalized.stock, now)
        return id
    }

    suspend fun delete(medication: MedicationEntity) {
        doseLogDao.deleteForMedication(medication.id)
        stockOutageDao.deleteForMedication(medication.id)
        medicationDao.delete(medication)
    }

    suspend fun getMedication(id: Long) = medicationDao.getById(id)
    suspend fun activeMedications() = medicationDao.getActive()
    suspend fun getDoseLog(medicationId: Long, scheduledAt: Long) =
        doseLogDao.getForDose(medicationId, scheduledAt)
    suspend fun refill(id: Long, amount: Int): MedicationEntity? {
        if (amount <= 0) return medicationDao.getById(id)
        val medication = medicationDao.getById(id) ?: return null
        val updatedStock = medication.stock + amount
        medicationDao.setStock(id, updatedStock, "")
        if (medication.stock <= 0 && updatedStock > 0) {
            stockOutageDao.getOpen(id)?.let { stockOutageDao.close(it.id, System.currentTimeMillis()) }
        }
        return medicationDao.getById(id)
    }

    suspend fun acknowledgeStockAlert(medication: MedicationEntity) {
        medicationDao.acknowledgeStockAlert(medication.id, medication.stockLevel.name)
    }

    suspend fun recordDose(
        dose: DailyDose,
        status: DoseStatus,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val currentMedication = medicationDao.getById(dose.medication.id) ?: return false
        val existing = doseLogDao.getForDose(dose.medication.id, dose.scheduledAt)
        val previous = existing?.status
            ?.let { runCatching { DoseStatus.valueOf(it) }.getOrNull() }
            ?: dose.status
        val effectiveDose = dose.copy(
            medication = currentMedication,
            status = previous,
            logId = existing?.id ?: dose.logId,
            actionAt = existing?.actionAt ?: dose.actionAt
        )

        if ((status == DoseStatus.TAKEN || status == DoseStatus.POSTPONED) &&
            !effectiveDose.canBeTakenAt(now)
        ) {
            return false
        }
        if (status == DoseStatus.MISSED && previous == DoseStatus.TAKEN) return false
        if (status == previous && status != DoseStatus.POSTPONED) return false

        doseLogDao.insert(
            DoseLogEntity(
                id = existing?.id ?: dose.logId ?: 0,
                medicationId = dose.medication.id,
                scheduledAt = dose.scheduledAt,
                status = status.name,
                actionAt = now
            )
        )
        if (status == DoseStatus.TAKEN && previous != DoseStatus.TAKEN) {
            val updatedStock = (currentMedication.stock - currentMedication.pillsPerDose).coerceAtLeast(0)
            val nextLevel = currentMedication.copy(stock = updatedStock).stockLevel
            val acknowledgement = if (nextLevel == currentMedication.stockLevel) {
                currentMedication.acknowledgedStockLevel
            } else ""
            medicationDao.setStock(currentMedication.id, updatedStock, acknowledgement)
            updateOutageForStockChange(
                currentMedication.id,
                currentMedication.stock,
                updatedStock,
                now
            )
        }
        return true
    }

    suspend fun reconcileMissedDoses(now: Long = System.currentTimeMillis()) {
        val cutoff = now - DAY_MS
        medicationDao.getAll().forEach { medication ->
            var day = maxOf(
                medication.normalizedTreatmentStartDate,
                medication.trackingResumedAt.takeIf { it > 0 } ?: medication.normalizedTreatmentStartDate
            )
            val lastDay = startOfDay(cutoff)
            while (day <= lastDay && medication.isTreatmentDay(day)) {
                medication.times.forEach { time ->
                    val scheduledAt = timeOnDay(day, time) ?: return@forEach
                    val beforeCancellation = medication.deactivatedAt <= 0 || scheduledAt < medication.deactivatedAt
                    if (beforeCancellation && scheduledAt + DAY_MS <= now) {
                        val existing = doseLogDao.getForDose(medication.id, scheduledAt)
                        val resolved = existing?.status == DoseStatus.TAKEN.name ||
                            existing?.status == DoseStatus.MISSED.name
                        if (!resolved) {
                            doseLogDao.insert(
                                DoseLogEntity(
                                    id = existing?.id ?: 0,
                                    medicationId = medication.id,
                                    scheduledAt = scheduledAt,
                                    status = DoseStatus.MISSED.name,
                                    actionAt = scheduledAt + DAY_MS
                                )
                            )
                        }
                    }
                }
                day = Calendar.getInstance().apply {
                    timeInMillis = day
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            }
        }
    }

    private suspend fun updateOutageForStockChange(
        medicationId: Long,
        previousStock: Int?,
        newStock: Int,
        now: Long
    ) {
        if (newStock <= 0 && (previousStock == null || previousStock > 0)) {
            if (stockOutageDao.getOpen(medicationId) == null) {
                stockOutageDao.insert(StockOutageEntity(medicationId = medicationId, startedAt = now))
            }
        } else if (newStock > 0 && previousStock != null && previousStock <= 0) {
            stockOutageDao.getOpen(medicationId)?.let { stockOutageDao.close(it.id, now) }
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
