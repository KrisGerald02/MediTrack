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
