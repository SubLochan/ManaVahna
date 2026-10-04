package com.manavahana.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.manavahana.R
import com.manavahana.data.database.AppDatabase
import com.manavahana.ui.QuickAddFuelActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

object FuelWidgetUpdater {

    const val ACTION_REFRESH_WIDGET = "com.manavahana.action.REFRESH_WIDGET"
    const val EXTRA_PRESET_AMOUNT = "extra_preset_amount"
    const val EXTRA_TARGET_VEHICLE_ID = "extra_target_vehicle_id"

    suspend fun updateAllWidgetsSuspend(context: Context) {
        withContext(Dispatchers.IO) {
            performUpdate(context)
        }
    }

    fun updateAllWidgets(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performUpdate(context)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    private suspend fun performUpdate(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FuelExpenseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            val db = AppDatabase.getDatabase(context)
            val vehicles = db.vehicleDao().getAllVehicles().firstOrNull() ?: emptyList()
            val allFuelLogs = db.fuelLogDao().getAllFuelLogs().firstOrNull() ?: emptyList()

            // Calculate current month start timestamp
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStart = calendar.timeInMillis

            val currentMonthFuelTotal = allFuelLogs
                .filter { it.fuelDate >= monthStart }
                .sumOf { it.totalAmount }

            val latestLog = allFuelLogs.maxByOrNull { it.fuelDate }
            val targetVehicle = if (latestLog != null) {
                vehicles.find { it.id == latestLog.vehicleId } ?: vehicles.firstOrNull()
            } else {
                vehicles.firstOrNull()
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val targetVehicleId = targetVehicle?.id ?: 0

            // 1. PendingIntent for main "Add Fuel Expense" data input action
            val addFuelIntent = Intent(context, QuickAddFuelActivity::class.java).apply {
                action = "com.manavahana.action.QUICK_ADD_FUEL"
                data = Uri.parse("manavahana://quickadd/main/$targetVehicleId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                targetVehicle?.let { putExtra(EXTRA_TARGET_VEHICLE_ID, it.id) }
            }
            val addFuelPendingIntent = PendingIntent.getActivity(context, 102, addFuelIntent, pendingIntentFlags)

            // 2. Preset ₹500
            val preset500Intent = Intent(context, QuickAddFuelActivity::class.java).apply {
                action = "com.manavahana.action.QUICK_ADD_FUEL_500"
                data = Uri.parse("manavahana://quickadd/preset/500/$targetVehicleId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_PRESET_AMOUNT, 500.0)
                targetVehicle?.let { putExtra(EXTRA_TARGET_VEHICLE_ID, it.id) }
            }
            val preset500PendingIntent = PendingIntent.getActivity(context, 103, preset500Intent, pendingIntentFlags)

            // 3. Preset ₹1000
            val preset1000Intent = Intent(context, QuickAddFuelActivity::class.java).apply {
                action = "com.manavahana.action.QUICK_ADD_FUEL_1000"
                data = Uri.parse("manavahana://quickadd/preset/1000/$targetVehicleId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_PRESET_AMOUNT, 1000.0)
                targetVehicle?.let { putExtra(EXTRA_TARGET_VEHICLE_ID, it.id) }
            }
            val preset1000PendingIntent = PendingIntent.getActivity(context, 104, preset1000Intent, pendingIntentFlags)

            // 4. Refresh widget broadcast
            val refreshIntent = Intent(context, FuelExpenseWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 105, refreshIntent, pendingIntentFlags)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_fuel_expense)

                // Bind Vehicle info
                if (targetVehicle != null) {
                    views.setTextViewText(R.id.widget_vehicle_name, targetVehicle.vehicleName)
                    views.setTextViewText(
                        R.id.widget_vehicle_number,
                        "${targetVehicle.vehicleNumber} • ${targetVehicle.fuelType}"
                    )
                } else {
                    views.setTextViewText(R.id.widget_vehicle_name, "ManaVahana Fuel Tracker")
                    views.setTextViewText(R.id.widget_vehicle_number, "Tap to log fuel expense")
                }

                // Bind Stats
                if (latestLog != null) {
                    val costFormatted = String.format(Locale.getDefault(), "₹%.0f (%.1fL)", latestLog.totalAmount, latestLog.litersFilled)
                    views.setTextViewText(R.id.widget_last_fuel_cost, costFormatted)
                    val odoFormatted = String.format(Locale.getDefault(), "%.0f km", latestLog.odometerReading)
                    views.setTextViewText(R.id.widget_odometer_text, odoFormatted)
                } else {
                    views.setTextViewText(R.id.widget_last_fuel_cost, "None yet")
                    views.setTextViewText(R.id.widget_odometer_text, "0 km")
                }

                val monthTotalFormatted = String.format(Locale.getDefault(), "₹%,.0f", currentMonthFuelTotal)
                views.setTextViewText(R.id.widget_month_fuel_total, monthTotalFormatted)

                // Bind click listeners: ALL taps on the widget accept data input directly from home screen
                views.setOnClickPendingIntent(R.id.widget_root, addFuelPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_header_row, addFuelPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_vehicle_container, addFuelPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_stats_row, addFuelPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_add_fuel, addFuelPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_preset_500, preset500PendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_preset_1000, preset1000PendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
