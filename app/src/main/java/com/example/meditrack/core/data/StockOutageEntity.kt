package com.example.meditrack.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_outages",
    indices = [Index(value = ["medicationId"])]
)
data class StockOutageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val startedAt: Long,
    val endedAt: Long? = null
)
