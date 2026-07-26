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
            val name = "ManaVahana Reminders"
            val descriptionText = "Notifications for vehicle maintenance, insurance expiries, and service renewals"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("manavahana_reminders", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupPeriodicReminders() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                1, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "manavahana_reminder_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
