package com.example.meditrack.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MedicationEntity::class, DoseLogEntity::class], version = 3, exportSchema = false)
abstract class MediTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile private var instance: MediTrackDatabase? = null

        fun getInstance(context: Context): MediTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MediTrackDatabase::class.java,
                "meditrack.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN amount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medications ADD COLUMN unit TEXT NOT NULL DEFAULT 'mg'")
                db.execSQL("ALTER TABLE medications ADD COLUMN pillsPerDose INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE medications ADD COLUMN category TEXT NOT NULL DEFAULT 'MEDICINE'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE dose_logs SET status = 'POSTPONED' WHERE status = 'SKIPPED'")
            }
        }
    }
}
