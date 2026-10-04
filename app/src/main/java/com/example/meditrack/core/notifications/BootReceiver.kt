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
            app.repository.activeMedications().forEach { medication ->
                scheduler.schedule(medication)
                medication.times.forEach { time ->
                    val scheduledAt = MedicationRepository.timeOnDay(today, time) ?: return@forEach
                    if (scheduledAt <= now) {
                        val status = app.repository.getDoseLog(medication.id, scheduledAt)?.status
                        if (status != DoseStatus.TAKEN.name && status != DoseStatus.SKIPPED.name) {
                            scheduler.scheduleRepeat(medication, time, scheduledAt)
                        }
                    }
                }
            }
            result.finish()
        }
    }
}
