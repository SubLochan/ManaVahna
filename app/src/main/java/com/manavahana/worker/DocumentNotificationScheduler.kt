package com.manavahana.worker

import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.manavahana.data.model.Document
import java.util.concurrent.TimeUnit

object DocumentNotificationScheduler {

    const val EXTRA_DOCUMENT_ID = "document_id"
    const val EXTRA_ALERT_TYPE = "alert_type"
    const val EXTRA_EXPECTED_EXPIRY = "expected_expiry"

    fun reminderNotificationId(reminderId: Int): Int = reminderId + 10000
    fun vehicleInsuranceNotificationId(vehicleId: Int): Int = vehicleId + 20000
    fun vehiclePollutionNotificationId(vehicleId: Int): Int = vehicleId + 30000
    fun documentNotificationId(documentId: Int): Int = documentId + 40000

    private fun uniqueWorkName(documentId: Int, alertType: String): String =
        "doc_expiry_${documentId}_$alertType"

    fun scheduleExpiryNotifications(context: Context, document: Document) {
        val appContext = context.applicationContext ?: context
        val expiry = document.expiryDate
        if (expiry == null || expiry <= 0L) {
            cancelScheduledNotifications(appContext, document.id)
            cancelDocumentNotification(appContext, document.id)
            return
        }

        val currentTime = System.currentTimeMillis()

        // Clean up any previously scheduled notifications and active tray alerts for this document
        cancelScheduledNotifications(appContext, document.id)
        if (expiry - currentTime > 7 * 24 * 60 * 60 * 1000L) {
            cancelDocumentNotification(appContext, document.id)
        }

        try {
            val workManager = WorkManager.getInstance(appContext)
            val alerts = listOf(
                "7_days_before" to (expiry - 7 * 24 * 60 * 60 * 1000L),
                "1_day_before" to (expiry - 24 * 60 * 60 * 1000L),
                "exact_day" to expiry
            )

            for ((alertType, triggerAt) in alerts) {
                if (triggerAt > currentTime) {
                    val delay = triggerAt - currentTime
                    val data = Data.Builder()
                        .putInt(EXTRA_DOCUMENT_ID, document.id)
                        .putString(EXTRA_ALERT_TYPE, alertType)
                        .putLong(EXTRA_EXPECTED_EXPIRY, expiry)
                        .build()
                    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag("doc_expiry_${document.id}")
                        .setInputData(data)
                        .build()
                    workManager.enqueueUniqueWork(
                        uniqueWorkName(document.id, alertType),
                        ExistingWorkPolicy.REPLACE,
                        request
                    )
                }
            }
        } catch (_: Exception) {
            // WorkManager may not be initialized in unit test environments
        }
    }

    fun cancelScheduledNotifications(context: Context, documentId: Int) {
        try {
            val appContext = context.applicationContext ?: context
            val workManager = WorkManager.getInstance(appContext)
            workManager.cancelAllWorkByTag("doc_expiry_$documentId")
            workManager.cancelUniqueWork(uniqueWorkName(documentId, "7_days_before"))
            workManager.cancelUniqueWork(uniqueWorkName(documentId, "1_day_before"))
            workManager.cancelUniqueWork(uniqueWorkName(documentId, "exact_day"))
        } catch (_: Exception) {
            // WorkManager may not be initialized in unit test environments
        }
    }

    fun cancelNotification(context: Context?, notificationId: Int) {
        if (context == null) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(notificationId)
        } catch (_: Exception) {
            // Ignore in unit test environments
        }
    }

    fun cancelDocumentNotification(context: Context?, documentId: Int) {
        cancelNotification(context, documentNotificationId(documentId))
    }

    fun cancelReminderNotification(context: Context?, reminderId: Int) {
        cancelNotification(context, reminderNotificationId(reminderId))
    }

    fun cancelVehicleExpiryNotifications(
        context: Context?,
        vehicleId: Int,
        cancelInsurance: Boolean = true,
        cancelPollution: Boolean = true
    ) {
        if (cancelInsurance) {
            cancelNotification(context, vehicleInsuranceNotificationId(vehicleId))
        }
        if (cancelPollution) {
            cancelNotification(context, vehiclePollutionNotificationId(vehicleId))
        }
    }

    fun cancelAllNotifications(context: Context?) {
        if (context == null) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancelAll()
        } catch (_: Exception) {
            // Ignore in unit test environments
        }
    }
}
