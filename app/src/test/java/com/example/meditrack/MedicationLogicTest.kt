package com.example.meditrack

import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.MedicationRepository
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
    fun displayDose_includes_pills_amount_and_unit() {
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

        assertEquals("2 pastillas · 500 mg", medication.displayDose)
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
