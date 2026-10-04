package com.example.meditrack

import android.app.Application
import com.example.meditrack.core.data.MediTrackDatabase
import com.example.meditrack.core.data.MedicationRepository
import com.example.meditrack.core.data.UserPreferences
import com.example.meditrack.core.notifications.NotificationHelper

class MediTrackApplication : Application() {
    val database by lazy { MediTrackDatabase.getInstance(this) }
    val repository by lazy { MedicationRepository(database.medicationDao(), database.doseLogDao()) }
    val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
