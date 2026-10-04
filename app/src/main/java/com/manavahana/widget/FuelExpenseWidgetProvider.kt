package com.manavahana.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class FuelExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        FuelWidgetUpdater.updateAllWidgets(context) {
            try {
                pendingResult.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val pendingResult = goAsync()
        FuelWidgetUpdater.updateAllWidgets(context) {
            try {
                pendingResult.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        FuelWidgetUpdater.updateAllWidgets(context) {
            try {
                pendingResult.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == FuelWidgetUpdater.ACTION_REFRESH_WIDGET ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val pendingResult = goAsync()
            FuelWidgetUpdater.updateAllWidgets(context) {
                if (action == FuelWidgetUpdater.ACTION_REFRESH_WIDGET) {
                    try {
                        Toast.makeText(context, "Fuel Widget Refreshed!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                try {
                    pendingResult.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }
}
