package com.manavahana.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manavahana.MainActivity
import com.manavahana.ManaVahanaApplication
import com.manavahana.data.model.Vehicle
import com.manavahana.data.model.Document
import com.manavahana.data.model.Reminder
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

        // Check for notifications permission first on newer devices
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val status = androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // If not granted, fail gracefully rather than crashing or throwing errors
                return Result.success()
            }
        }

        val currentTime = System.currentTimeMillis()
        val notifyThreshold = 7 * 24 * 60 * 60 * 1000L // 7 days ahead for proactive warning
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // 1. Check Explicit Reminders
        val reminders = repository.pendingReminders.firstOrNull() ?: emptyList()
        for (reminder in reminders) {
            val diff = reminder.reminderDate - currentTime
            val reminderTitle = reminder.title
            if (diff in 0..notifyThreshold) {
                sendNotification(
                    id = reminder.id + 10000,
                    title = "ManaVahana: $reminderTitle (త్వరలో ఉంది/Upcoming)",
                    message = reminder.description.ifEmpty { "మీకు ఒక రిమైండర్ టాస్క్ ఉంది." }
                )
            } else if (diff < 0) {
                sendNotification(
                    id = reminder.id + 10000,
                    title = "ManaVahana: $reminderTitle (గడువు ముగిసింది/Overdue)",
                    message = "గడువు తేదీ: ${sdf.format(Date(reminder.reminderDate))}. ${reminder.description}"
                )
            }
        }

        // 2. Check Vehicle Expiries (Insurance & Pollution)
        val vehicles = repository.allVehicles.firstOrNull() ?: emptyList()
        for (vehicle in vehicles) {
            // Insurance Expiry check
            if (vehicle.insuranceExpiry > 0) {
                val insDiff = vehicle.insuranceExpiry - currentTime
                if (insDiff in 0..notifyThreshold) {
                    sendNotification(
                        id = vehicle.id + 20000,
                        title = "${vehicle.vehicleName} ఇన్సూరెన్స్ త్వరలో ముగియనుంది",
                        message = "మీ ఇన్సూరెన్స్ ${sdf.format(Date(vehicle.insuranceExpiry))} తేదీతో ముగియనుంది. ఇప్పుడే రిన్యూ చేసుకోండి!"
                    )
                } else if (insDiff < 0) {
                    sendNotification(
                        id = vehicle.id + 20000,
                        title = "${vehicle.vehicleName} ఇన్సూరెన్స్ గడువు ముగిసింది!",
                        message = "మీ వాహన ఇన్సూరెన్స్ ${sdf.format(Date(vehicle.insuranceExpiry))} తేదీతో ముగిసింది. దయచేసి వెంటనే రిన్యూ చేయండి!"
                    )
                }
            }

            // Pollution Expiry check
            if (vehicle.pollutionExpiry > 0) {
                val polDiff = vehicle.pollutionExpiry - currentTime
                if (polDiff in 0..notifyThreshold) {
                    sendNotification(
                        id = vehicle.id + 30000,
                        title = "${vehicle.vehicleName} పొల్యూషన్ సర్టిఫికేట్ అలర్ట్",
                        message = "మీ పొల్యూషన్ సర్టిఫికേట్ ${sdf.format(Date(vehicle.pollutionExpiry))} తో ముగియనుంది. తనిఖీ చేయించుకోండి!"
                    )
                } else if (polDiff < 0) {
                    sendNotification(
                        id = vehicle.id + 30000,
                        title = "${vehicle.vehicleName} పొల్యూషన్ గడువు ముగిసింది!",
                        message = "మీ పొల్యూషన్ సర్టిఫికేట్ ${sdf.format(Date(vehicle.pollutionExpiry))} నాటికి ముగిసింది. వెంటనే కొత్తది పొందండి!"
                    )
                }
            }
        }

        // 3. Check Document Expiries from vault repository
        val documents = repository.allDocuments.firstOrNull() ?: emptyList()
        for (document in documents) {
            val expiry = document.expiryDate
            if (expiry != null && expiry > 0) {
                val docDiff = expiry - currentTime
                if (docDiff in 0..notifyThreshold) {
                    sendNotification(
                        id = document.id + 40000,
                        title = "${document.docType}: ${document.title} త్వరలో ముగుస్తుంది",
                        message = "మీ డాక్యుమెంట్ గడువు ${sdf.format(Date(expiry))} తేదీతో ముగియనుంది!"
                    )
                } else if (docDiff < 0) {
                    sendNotification(
                        id = document.id + 40000,
                        title = "${document.docType}: ${document.title} గడువు ముగిసింది!",
                        message = "మీ సేవ్ చేసిన డాక్యుమెంట్ ${sdf.format(Date(expiry))} తేదీతో ముగిసింది. దయచేసి అప్‌డేట్ చేయండి!"
                    )
                }
            }
        }

        return Result.success()
    }

    private suspend fun sendNotification(id: Int, title: String, message: String) {
        val app = applicationContext as? ManaVahanaApplication
        val langCode = app?.userPreferencesRepository?.selectedLanguage?.firstOrNull() ?: "en"
        val translatedTitle = com.manavahana.ui.Localizer.translate(title, langCode)
        val translatedMessage = com.manavahana.ui.Localizer.translate(message, langCode)

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
            .setContentTitle(translatedTitle)
            .setContentText(translatedMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(translatedMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }
}
