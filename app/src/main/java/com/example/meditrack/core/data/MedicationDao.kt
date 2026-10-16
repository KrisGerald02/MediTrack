package com.example.meditrack.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY active DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE active = 1")
    suspend fun getActive(): List<MedicationEntity>

    @Query("SELECT * FROM medications")
    suspend fun getAll(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("UPDATE medications SET stock = MAX(0, stock - :amount) WHERE id = :id")
    suspend fun consume(id: Long, amount: Int)

    @Query("UPDATE medications SET stock = stock + :amount WHERE id = :id")
    suspend fun addStock(id: Long, amount: Int)

    @Query("UPDATE medications SET stock = :stock, acknowledgedStockLevel = :acknowledgedLevel WHERE id = :id")
    suspend fun setStock(id: Long, stock: Int, acknowledgedLevel: String)

    @Query("UPDATE medications SET acknowledgedStockLevel = :level WHERE id = :id")
    suspend fun acknowledgeStockAlert(id: Long, level: String)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY scheduledAt DESC")
    fun observeAll(): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DoseLogEntity): Long

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun getForDose(medicationId: Long, scheduledAt: Long): DoseLogEntity?

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}

@Dao
interface StockOutageDao {
    @Query("SELECT * FROM stock_outages ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<StockOutageEntity>>

    @Insert
    suspend fun insert(outage: StockOutageEntity): Long

    @Query("SELECT * FROM stock_outages WHERE medicationId = :medicationId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpen(medicationId: Long): StockOutageEntity?

    @Query("UPDATE stock_outages SET endedAt = :endedAt WHERE id = :id")
    suspend fun close(id: Long, endedAt: Long)

    @Query("DELETE FROM stock_outages WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}
