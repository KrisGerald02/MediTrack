package com.example.meditrack

import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.MedicationRepository
import com.example.meditrack.core.data.DailyDose
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.data.StockLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MedicationLogicTest {
    @Test
    fun times_are_trimmed_filtered_and_sorted() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 tableta",
            timesCsv = "20:00, 08:00, , 14:30",
            stock = 10,
            lowStockThreshold = 2
        )

        assertEquals(listOf("08:00", "14:30", "20:00"), medication.times)
    }

    @Test
    fun displayDose_includes_only_pills_per_dose() {
        val medication = MedicationEntity(
            name = "Suplemento",
            dose = "legacy",
            amount = "500",
            unit = "mg",
            pillsPerDose = 2,
            timesCsv = "08:00",
            stock = 20,
            lowStockThreshold = 4
        )

        assertEquals("2 pastillas", medication.displayDose)
    }

    @Test
    fun reminder_interval_uses_configured_minutes() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            reminderIntervalMinutes = 12,
            timesCsv = "08:00",
            stock = 10,
            lowStockThreshold = 2
        )

        assertEquals(12 * 60_000L, medication.reminderIntervalMillis)
    }

    @Test
    fun pending_dose_cannot_be_taken_before_its_scheduled_time() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            stock = 10,
            lowStockThreshold = 2
        )
        val scheduledAt = 1_000_000L
        val dose = DailyDose(medication, scheduledAt, "08:00", DoseStatus.PENDING)

        assertEquals(false, dose.canBeTakenAt(scheduledAt - 1))
        assertEquals(true, dose.canBeTakenAt(scheduledAt))
    }

    @Test
    fun postponed_dose_uses_action_time_plus_reminder_interval() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            reminderIntervalMinutes = 5,
            stock = 10,
            lowStockThreshold = 2
        )
        val postponedAt = 2_000_000L
        val dose = DailyDose(
            medication = medication,
            scheduledAt = 1_000_000L,
            time = "08:00",
            status = DoseStatus.POSTPONED,
            actionAt = postponedAt
        )

        assertEquals(postponedAt + 5 * 60_000L, dose.availableAt)
        assertEquals(false, dose.canBeTakenAt(dose.availableAt - 1))
        assertEquals(true, dose.canBeTakenAt(dose.availableAt))
    }

    @Test
    fun dose_expires_exactly_24_hours_after_its_original_schedule() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            stock = 10,
            lowStockThreshold = 2
        )
        val scheduledAt = 1_000_000L
        val dose = DailyDose(medication, scheduledAt, "08:00", DoseStatus.PENDING)

        assertEquals(false, dose.isExpiredAt(scheduledAt + MedicationRepository.DAY_MS - 1))
        assertEquals(true, dose.isExpiredAt(scheduledAt + MedicationRepository.DAY_MS))
        assertEquals(false, dose.canBeTakenAt(scheduledAt + MedicationRepository.DAY_MS))
    }

    @Test
    fun dose_cannot_be_taken_when_inventory_is_zero() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            stock = 0,
            lowStockThreshold = 2
        )
        val dose = DailyDose(medication, 1_000L, "08:00", DoseStatus.PENDING)

        assertEquals(false, dose.canBeTakenAt(2_000L))
    }

    @Test
    fun stock_levels_include_out_low_medium_and_good() {
        fun medication(stock: Int) = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            stock = stock,
            lowStockThreshold = 5
        )

        assertEquals(StockLevel.OUT, medication(0).stockLevel)
        assertEquals(StockLevel.LOW, medication(5).stockLevel)
        assertEquals(StockLevel.MEDIUM, medication(10).stockLevel)
        assertEquals(StockLevel.GOOD, medication(11).stockLevel)
    }

    @Test
    fun acknowledged_alert_stays_hidden_until_stock_level_changes() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            timesCsv = "08:00",
            stock = 8,
            lowStockThreshold = 5,
            acknowledgedStockLevel = StockLevel.MEDIUM.name
        )

        assertEquals(false, medication.hasUnacknowledgedStockAlert)
        assertEquals(true, medication.copy(stock = 5).hasUnacknowledgedStockAlert)
    }

    @Test
    fun dose_interval_generates_daily_times_from_start_time() {
        val medication = MedicationEntity(
            name = "Medicamento",
            dose = "1 pastilla",
            doseIntervalHours = 4,
            timesCsv = "08:00",
            stock = 10,
            lowStockThreshold = 2
        )

        assertEquals(
            listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00"),
            medication.times
        )
        assertEquals("Cada 4 h · inicia 08:00", medication.scheduleDescription)
    }

    @Test
    fun finite_treatment_includes_start_and_last_day_only() {
        val start = MedicationRepository.startOfDay(System.currentTimeMillis())
        val medication = MedicationEntity(
            name = "Antibiótico",
            dose = "1 pastilla",
            treatmentStartDate = start,
            treatmentDurationDays = 3,
            timesCsv = "08:00",
            stock = 10,
            lowStockThreshold = 2
        )

        assertEquals(true, medication.isTreatmentDay(start))
        assertEquals(true, medication.isTreatmentDay(start + 2 * MedicationRepository.DAY_MS))
        assertEquals(false, medication.isTreatmentDay(start + 3 * MedicationRepository.DAY_MS))
    }

    @Test
    fun timeOnDay_builds_expected_local_time() {
        val dayStart = MedicationRepository.startOfDay(System.currentTimeMillis())
        val result = MedicationRepository.timeOnDay(dayStart, "14:30")
        assertNotNull(result)
        val calendar = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun timeOnDay_rejects_invalid_values() {
        val dayStart = MedicationRepository.startOfDay(System.currentTimeMillis())
        assertNull(MedicationRepository.timeOnDay(dayStart, "25:00"))
        assertNull(MedicationRepository.timeOnDay(dayStart, "sin-hora"))
    }
}
