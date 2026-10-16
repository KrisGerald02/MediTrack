package com.example.meditrack.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicationEntity::class, DoseLogEntity::class, StockOutageEntity::class],
    version = 7,
    exportSchema = false
)
abstract class MediTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun stockOutageDao(): StockOutageDao

    companion object {
        @Volatile private var instance: MediTrackDatabase? = null

        fun getInstance(context: Context): MediTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MediTrackDatabase::class.java,
                "meditrack.db"
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            ).build().also { instance = it }
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN reminderIntervalMinutes INTEGER NOT NULL DEFAULT 5")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN doseIntervalHours INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN treatmentStartDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medications ADD COLUMN treatmentDurationDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE medications SET treatmentStartDate = createdAt WHERE treatmentStartDate = 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN deactivatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medications ADD COLUMN trackingResumedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medications ADD COLUMN acknowledgedStockLevel TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS stock_outages (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "medicationId INTEGER NOT NULL, " +
                        "startedAt INTEGER NOT NULL, " +
                        "endedAt INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_outages_medicationId ON stock_outages (medicationId)")
                db.execSQL(
                    "INSERT INTO stock_outages (medicationId, startedAt, endedAt) " +
                        "SELECT id, createdAt, NULL FROM medications WHERE stock <= 0"
                )
            }
        }
    }
}
