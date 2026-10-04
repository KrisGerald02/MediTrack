package com.example.meditrack.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val amount: String = "",
    val unit: String = "mg",
    val pillsPerDose: Int = 1,
    val category: String = "MEDICINE",
    val instructions: String = "",
    val timesCsv: String,
    val stock: Int,
    val lowStockThreshold: Int,
    val colorIndex: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val times: List<String>
        get() = timesCsv.split(",").map(String::trim).filter(String::isNotBlank).sorted()

    val displayDose: String
        get() = if (amount.isBlank()) dose else {
            val form = if (pillsPerDose == 1) "1 pastilla" else "$pillsPerDose pastillas"
            "$form · $amount $unit"
        }
}

enum class TreatmentCategory { MEDICINE, SUPPLEMENT }
