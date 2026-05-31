package com.example.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.ManaVahanaApplication
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ManaVahanaApplication ?: return Result.failure()
        val repository = app.repository

        // 1. Check Explicit Reminders
        val reminders = repository.pendingReminders.firstOrNull() ?: emptyList()
        val currentTime = System.currentTimeMillis()
        val notifyThreshold = 3 * 24 * 60 * 60 * 1000L // 3 days

        var notificationId = 100

        for (reminder in reminders) {
            val diff = reminder.reminderDate - currentTime
            if (diff in 0..notifyThreshold) {
                sendNotification(
                    id = reminder.id + 1000,
                    title = "ManaVahana: ${reminder.title}",
                    message = reminder.description
                )
            }
        }

        // 2. Check Vehicle Expiries (Insurance & Pollution)
        val vehicles = repository.allVehicles.firstOrNull() ?: emptyList()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        for (vehicle in vehicles) {
            // Insurance Expiry check
            if (vehicle.insuranceExpiry > 0) {
                val insDiff = vehicle.insuranceExpiry - currentTime
                if (insDiff in 0..notifyThreshold) {
                    sendNotification(
                        id = notificationId++,
                        title = "${vehicle.vehicleName} Insurance Expiry Alert",
                        message = "Your insurance is expiring on ${sdf.format(Date(vehicle.insuranceExpiry))}. Tap to renew!"
                    )
                }
            }

            // Pollution Expiry check
            if (vehicle.pollutionExpiry > 0) {
                val polDiff = vehicle.pollutionExpiry - currentTime
                if (polDiff in 0..notifyThreshold) {
                    sendNotification(
                        id = notificationId++,
                        title = "${vehicle.vehicleName} Pollution Certificate Alert",
                        message = "Your PUC expires on ${sdf.format(Date(vehicle.pollutionExpiry))}. Get it checked soon!"
                    )
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "manavahana_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }
}
