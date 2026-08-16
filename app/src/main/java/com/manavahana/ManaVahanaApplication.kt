package com.manavahana

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.manavahana.data.database.AppDatabase
import com.manavahana.data.preferences.UserPreferencesRepository
import com.manavahana.data.repository.ManaVahanaRepository
import com.manavahana.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class ManaVahanaApplication : Application() {

    // Lazy initialization for Dependency Injection Container
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ManaVahanaRepository(
            database.vehicleDao(),
            database.serviceLogDao(),
            database.fuelLogDao(),
            database.expenseDao(),
            database.documentDao(),
            database.reminderDao(),
            database.userDao()
        )
    }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPeriodicReminders()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                "manavahana_reminders",
                "ManaVahana Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for vehicle maintenance, insurance expiries, and service renewals"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val updateChannel = NotificationChannel(
                "app_update_channel",
                "ManaVahana App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for available software and system updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(updateChannel)
        }
    }

    private fun setupPeriodicReminders() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "manavahana_reminder_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
