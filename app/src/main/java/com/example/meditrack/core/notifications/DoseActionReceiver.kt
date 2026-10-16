package com.example.meditrack.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.core.data.DailyDose
import com.example.meditrack.core.data.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val time = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_TIME) ?: return
        val scheduledAt = intent.getLongExtra(
            MedicationAlarmScheduler.EXTRA_SCHEDULED_AT,
            System.currentTimeMillis()
        )
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val status = when (intent.action) {
            ACTION_TAKEN -> DoseStatus.TAKEN
            ACTION_POSTPONE -> DoseStatus.POSTPONED
            else -> return
        }

        NotificationManagerCompat.from(context).cancel(notificationId)
        val result = goAsync()
        val app = context.applicationContext as MediTrackApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.getMedication(medicationId)?.let { medication ->
                val recorded = app.repository.recordDose(
                    DailyDose(
                        medication = medication,
                        scheduledAt = scheduledAt,
                        time = time,
                        status = DoseStatus.PENDING
                    ),
                    status
                )
                if (!recorded) {
                    result.finish()
                    return@launch
                } else if (status == DoseStatus.POSTPONED) {
                    MedicationAlarmScheduler(context).scheduleRepeat(medication, time, scheduledAt)
                } else {
                    MedicationAlarmScheduler(context).cancelRepeat(medication, time, scheduledAt)
                    app.repository.getMedication(medicationId)?.takeIf { it.stock <= 0 }?.let {
                        MedicationAlarmScheduler(context).cancel(it)
                    }
                }
            }
            result.finish()
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.example.meditrack.action.TAKEN"
        const val ACTION_POSTPONE = "com.example.meditrack.action.POSTPONE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
