package com.example.meditrack.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meditrack.MediTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1)
        val name = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_NAME) ?: return
        val dose = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_DOSE).orEmpty()
        val time = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_TIME) ?: return
        val scheduledAt = intent.getLongExtra(
            MedicationAlarmScheduler.EXTRA_SCHEDULED_AT,
            System.currentTimeMillis()
        )
        val result = goAsync()
        val app = context.applicationContext as MediTrackApplication
        CoroutineScope(Dispatchers.IO).launch {
            val medication = app.repository.getMedication(id)
            if (medication != null) {
                val scheduler = MedicationAlarmScheduler(context)
                if (!medication.active || medication.stock <= 0 || !medication.isTreatmentDay(scheduledAt)) {
                    scheduler.cancelRepeat(medication, time, scheduledAt)
                    result.finish()
                    return@launch
                }
                if (System.currentTimeMillis() >= scheduledAt + com.example.meditrack.core.data.MedicationRepository.DAY_MS) {
                    app.repository.recordDose(
                        com.example.meditrack.core.data.DailyDose(
                            medication = medication,
                            scheduledAt = scheduledAt,
                            time = time,
                            status = com.example.meditrack.core.data.DoseStatus.PENDING
                        ),
                        com.example.meditrack.core.data.DoseStatus.MISSED
                    )
                    scheduler.cancelRepeat(medication, time, scheduledAt)
                    result.finish()
                    return@launch
                }
                val status = app.repository.getDoseLog(id, scheduledAt)?.status
                val resolved = status == com.example.meditrack.core.data.DoseStatus.TAKEN.name ||
                    status == com.example.meditrack.core.data.DoseStatus.SKIPPED.name ||
                    status == com.example.meditrack.core.data.DoseStatus.MISSED.name
                if (resolved) {
                    scheduler.cancelRepeat(medication, time, scheduledAt)
                } else {
                    NotificationHelper.showReminder(context, id, name, dose, time, scheduledAt)
                    if (!intent.getBooleanExtra(MedicationAlarmScheduler.EXTRA_IS_REPEAT, false)) {
                        scheduler.scheduleTime(medication, time)
                    }
                    scheduler.scheduleRepeat(medication, time, scheduledAt)
                }
            }
            result.finish()
        }
    }
}
