package com.example.meditrack.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meditrack.MediTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.data.MedicationRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val result = goAsync()
        val app = context.applicationContext as MediTrackApplication
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = MedicationAlarmScheduler(context)
            val now = System.currentTimeMillis()
            val today = MedicationRepository.startOfDay(now)
            val yesterday = java.util.Calendar.getInstance().apply {
                timeInMillis = today
                add(java.util.Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
            app.repository.reconcileMissedDoses(now)
            app.repository.activeMedications().forEach { medication ->
                if (medication.stock <= 0) return@forEach
                scheduler.schedule(medication)
                listOf(yesterday, today).filter(medication::isTreatmentDay).forEach { day ->
                    medication.times.forEach { time ->
                        val scheduledAt = MedicationRepository.timeOnDay(day, time) ?: return@forEach
                        if (scheduledAt <= now) {
                            val status = app.repository.getDoseLog(medication.id, scheduledAt)?.status
                            if (scheduledAt + MedicationRepository.DAY_MS > now &&
                                status != DoseStatus.TAKEN.name && status != DoseStatus.SKIPPED.name &&
                                status != DoseStatus.MISSED.name
                            ) {
                                scheduler.scheduleRepeat(medication, time, scheduledAt)
                            }
                        }
                    }
                }
            }
            result.finish()
        }
    }
}
