package com.manavahana.worker

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.manavahana.data.model.Document
import java.util.concurrent.TimeUnit

object DocumentNotificationScheduler {
    fun scheduleExpiryNotifications(context: Context, document: Document) {
        val expiry = document.expiryDate ?: return
        val currentTime = System.currentTimeMillis()
        val workManager = WorkManager.getInstance(context)

        // Clean up any previously scheduled notifications for this specific document model
        cancelScheduledNotifications(context, document.id)

        // 1. Alert 7 days before
        val sevenDaysBefore = expiry - 7 * 24 * 60 * 60 * 1000L
        if (sevenDaysBefore > currentTime) {
            val delay = sevenDaysBefore - currentTime
            val data = Data.Builder()
                .putInt("document_id", document.id)
                .putString("alert_type", "7_days_before")
                .build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("doc_expiry_${document.id}")
                .setInputData(data)
                .build()
            workManager.enqueue(request)
        }

        // 2. Alert 1 day before
        val oneDayBefore = expiry - 24 * 60 * 60 * 1000L
        if (oneDayBefore > currentTime) {
            val delay = oneDayBefore - currentTime
            val data = Data.Builder()
                .putInt("document_id", document.id)
                .putString("alert_type", "1_day_before")
                .build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("doc_expiry_${document.id}")
                .setInputData(data)
                .build()
            workManager.enqueue(request)
        }

        // 3. Alert on exact expiry day
        if (expiry > currentTime) {
            val delay = expiry - currentTime
            val data = Data.Builder()
                .putInt("document_id", document.id)
                .putString("alert_type", "exact_day")
                .build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("doc_expiry_${document.id}")
                .setInputData(data)
                .build()
            workManager.enqueue(request)
        }
    }

    fun cancelScheduledNotifications(context: Context, documentId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag("doc_expiry_$documentId")
    }
}
