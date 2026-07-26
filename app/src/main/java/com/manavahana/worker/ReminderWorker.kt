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

        // Check if this was a directed, scheduled document expiry alarm
        val docId = inputData.getInt("document_id", -1)
        if (docId != -1) {
            val alertType = inputData.getString("alert_type") ?: "exact_day"
            val documents = repository.allDocuments.firstOrNull() ?: emptyList()
            val document = documents.find { it.id == docId }
            if (document != null && document.expiryDate != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val formattedDate = sdf.format(Date(document.expiryDate))
                val title = when (alertType) {
                    "7_days_before" -> "${document.docType}: ${document.title} త్వరలో ముగియనుంది (Expires in 7 Days)"
                    "1_day_before" -> "${document.docType}: ${document.title} రేపటితో ముగియనుంది (Expires Tomorrow)"
                    else -> "${document.docType}: ${document.title} కాలపరిమితి ఈరోజే ముగుస్తుంది (Expires Today)"
                }
                val message = when (alertType) {
                    "7_days_before" -> "మీ సేవ్ చేసిన డాక్యుమెంట్ గడువు 7 రోజుల్లో (${formattedDate}) ముగుస్తుంది. దయచేసి అప్‌డేట్ చూసుకోండి!"
                    "1_day_before" -> "మీ సేవ్ చేసిన డాక్యుమెంట్ గడువు రేపటితో (${formattedDate}) ముగుస్తుంది. దయచేసి సిద్ధం చేసుకోండి!"
                    else -> "మీ సేవ్ చేసిన డాక్యుమెంట్ గడువు ఈరోజే (${formattedDate}) ముగిసింది. దయచేసి వెంటనే అప్‌డేట్ చేయండి!"
                }
                sendNotification(document.id + 40000, title, message)
            }
            return Result.success()
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

        // 4. Check for App Updates in Background
        try {
            val packageName = applicationContext.packageName
            val packageInfo = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    applicationContext.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    applicationContext.packageManager.getPackageInfo(packageName, 0)
                }
            } catch (e: Exception) {
                null
            }
            val realInstalledVersionName = packageInfo?.versionName ?: "1.0"

            val rawOverriddenApp = app.userPreferencesRepository.overriddenAppVersion.firstOrNull()
            val rawOverriddenPlayStore = app.userPreferencesRepository.overriddenPlayStoreVersion.firstOrNull()
            val latestVersionName = if (!rawOverriddenPlayStore.isNullOrBlank()) {
                rawOverriddenPlayStore
            } else {
                com.manavahana.ui.PlayStoreVersionFetcher.fetchVersion(packageName) ?: realInstalledVersionName
            }

            val currentVersionName = if (!rawOverriddenApp.isNullOrBlank()) {
                rawOverriddenApp
            } else if (latestVersionName != realInstalledVersionName) {
                com.manavahana.ui.PlayStoreVersionFetcher.getLowerVersion(latestVersionName)
            } else {
                realInstalledVersionName
            }

            if (isNewerVersion(currentVersionName, latestVersionName)) {
                val prefs = applicationContext.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
                val lastNotified = prefs.getString("last_notified_version_name", "")
                if (lastNotified != latestVersionName) {
                    sendUpdateNotification(latestVersionName)
                    prefs.edit().putString("last_notified_version_name", latestVersionName).apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val lateParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val length = maxOf(currParts.size, lateParts.size)
            for (i in 0 until length) {
                val currVal = currParts.getOrNull(i) ?: 0
                val lateVal = lateParts.getOrNull(i) ?: 0
                if (lateVal > currVal) return true
                if (currVal > lateVal) return false
            }
        } catch (e: Exception) {
            return latest != current
        }
        return false
    }

    private suspend fun sendUpdateNotification(version: String) {
        val app = applicationContext as? ManaVahanaApplication
        val rawLang = app?.userPreferencesRepository?.selectedLanguage?.firstOrNull()
        val langCode = if (rawLang.isNullOrEmpty()) "en" else rawLang

        val title = com.manavahana.ui.Localizer.get("update_available_title", langCode)
        val template = com.manavahana.ui.Localizer.get("update_available_desc", langCode)
        val desc = template.replace("%1\$s", version)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            7895,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_update_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "ManaVahana App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for available software and system updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(7895, notification)
    }

    private suspend fun sendNotification(id: Int, title: String, message: String) {
        val app = applicationContext as? ManaVahanaApplication
        val rawLang = app?.userPreferencesRepository?.selectedLanguage?.firstOrNull()
        val langCode = if (rawLang.isNullOrEmpty()) "en" else rawLang
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
