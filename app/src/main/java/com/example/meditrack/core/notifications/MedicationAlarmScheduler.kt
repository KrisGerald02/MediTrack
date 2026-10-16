package com.example.meditrack.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.MedicationRepository

class MedicationAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(medication: MedicationEntity) {
        if (!medication.active || medication.stock <= 0) return
        medication.times.forEach { time -> scheduleTime(medication, time) }
    }

    fun cancel(medication: MedicationEntity) {
        medication.times.forEach { time ->
            alarmManager.cancel(pendingIntent(medication, time, 0L, false))
        }
    }

    fun scheduleTime(medication: MedicationEntity, time: String, customAt: Long? = null) {
        if (!medication.active || medication.stock <= 0) return
        val now = System.currentTimeMillis()
        var day = maxOf(
            MedicationRepository.startOfDay(now),
            medication.normalizedTreatmentStartDate
        )
        var triggerAt = customAt ?: MedicationRepository.timeOnDay(day, time) ?: return
        if (triggerAt <= now) {
            val nextDay = java.util.Calendar.getInstance().apply {
                timeInMillis = day
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            day = nextDay.timeInMillis
            triggerAt = MedicationRepository.timeOnDay(day, time) ?: return
        }
        if (!medication.isTreatmentDay(triggerAt)) return

        val operation = pendingIntent(medication, time, triggerAt, false)
        setAlarm(triggerAt, operation)
    }

    fun scheduleRepeat(
        medication: MedicationEntity,
        time: String,
        scheduledAt: Long
    ) {
        if (!medication.active || medication.stock <= 0 || !medication.isTreatmentDay(scheduledAt)) return
        val triggerAt = System.currentTimeMillis() + medication.reminderIntervalMillis
        if (triggerAt >= scheduledAt + MedicationRepository.DAY_MS) return
        val operation = pendingIntent(medication, time, scheduledAt, true)
        // Posponer siempre reinicia el contador desde el momento de la acción.
        alarmManager.cancel(operation)
        setAlarm(triggerAt, operation)
    }

    fun cancelRepeat(medication: MedicationEntity, time: String, scheduledAt: Long) {
        alarmManager.cancel(pendingIntent(medication, time, scheduledAt, true))
    }

    private fun setAlarm(triggerAt: Long, operation: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun pendingIntent(
        medication: MedicationEntity,
        time: String,
        scheduledAt: Long,
        isRepeat: Boolean
    ): PendingIntent {
        val requestCode = if (isRepeat) {
            "${medication.id}-$scheduledAt-repeat".hashCode()
        } else {
            "${medication.id}-$time-daily".hashCode()
        }
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medication.id)
            putExtra(EXTRA_NAME, medication.name)
            putExtra(EXTRA_DOSE, medication.displayDose)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(EXTRA_IS_REPEAT, isRepeat)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_NAME = "name"
        const val EXTRA_DOSE = "dose"
        const val EXTRA_TIME = "time"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        const val EXTRA_IS_REPEAT = "is_repeat"
    }
}
