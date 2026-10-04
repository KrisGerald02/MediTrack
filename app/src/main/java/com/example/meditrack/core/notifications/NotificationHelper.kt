package com.example.meditrack.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.meditrack.MainActivity
import com.example.meditrack.R

object NotificationHelper {
    private const val CHANNEL_ID = "medication_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de medicación",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos para las dosis programadas"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        id: Long,
        name: String,
        dose: String,
        time: String,
        scheduledAt: Long
    ) {
        val notificationId = "$id-$scheduledAt".hashCode()
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val takenAction = actionIntent(context, id, time, scheduledAt, notificationId, DoseActionReceiver.ACTION_TAKEN)
        val postponeAction = actionIntent(context, id, time, scheduledAt, notificationId, DoseActionReceiver.ACTION_POSTPONE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Es hora de tu medicamento")
            .setContentText("$name · $dose")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Toma $dose de $name y registra el resultado en MediTrack."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_launcher, "Tomada", takenAction)
            .addAction(R.drawable.ic_launcher, "Posponer", postponeAction)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+: the in-app permission prompt may not have been accepted yet.
        }
    }

    private fun actionIntent(
        context: Context,
        medicationId: Long,
        time: String,
        scheduledAt: Long,
        notificationId: Int,
        action: String
    ): PendingIntent {
        val intent = Intent(context, DoseActionReceiver::class.java).apply {
            this.action = action
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationAlarmScheduler.EXTRA_TIME, time)
            putExtra(MedicationAlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(DoseActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            "$medicationId-$scheduledAt-$action".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
