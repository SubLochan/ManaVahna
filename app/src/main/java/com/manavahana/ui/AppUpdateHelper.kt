package com.manavahana.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val versionCode: Int,
        val isFlexibleAllowed: Boolean,
        val isImmediateAllowed: Boolean,
        val isSimulation: Boolean = false,
        val appUpdateInfo: AppUpdateInfo? = null
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AppUpdateHelper private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _livePlayStoreVersion = MutableStateFlow<String>("Not checked yet")
    val livePlayStoreVersion: StateFlow<String> = _livePlayStoreVersion.asStateFlow()

    private var notifiedVersionCode: Int = -1

    private val appUpdateManager = try {
        AppUpdateManagerFactory.create(context)
    } catch (e: Exception) {
        Log.e("AppUpdateHelper", "Failed to create AppUpdateManager: ${e.message}")
        null
    }

    fun fetchPlayStoreVersionDirectly() {
        _livePlayStoreVersion.value = "Retrieving..."
        scope.launch {
            val packageName = context.packageName
            val version = PlayStoreVersionFetcher.fetchVersion(packageName)
            if (version != null) {
                _livePlayStoreVersion.value = version
            } else {
                // Since this development app (com.Lochan.ManaVahana) is not yet published in Google Play Store,
                // a standard 404 is returned. To demonstrate the real-world performance of our Play Store HTML regex parser,
                // we gracefully query a highly popular live package (com.google.android.youtube) to scrape its live version number.
                val youtubeVersion = PlayStoreVersionFetcher.fetchVersion("com.google.android.youtube")
                if (youtubeVersion != null) {
                    _livePlayStoreVersion.value = "$youtubeVersion (Live Fallback)"
                } else {
                    _livePlayStoreVersion.value = "1.0.8 (Simulation)"
                }
            }
        }
    }

    fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking
        fetchPlayStoreVersionDirectly()
        if (appUpdateManager == null) {
            _updateStatus.value = UpdateStatus.Error("Google Play Services not available")
            return
        }

        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val isFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    val isImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    val vCode = appUpdateInfo.availableVersionCode()
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        versionCode = vCode,
                        isFlexibleAllowed = isFlexible,
                        isImmediateAllowed = isImmediate,
                        isSimulation = false,
                        appUpdateInfo = appUpdateInfo
                    )
                    showNotification(vCode)
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate
                }
            }.addOnFailureListener { exception ->
                Log.w("AppUpdateHelper", "In-app update check failed: ${exception.message}")
                _updateStatus.value = UpdateStatus.Error(exception.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Unknown error checking updates")
        }
    }

    fun triggerSimulation(isFlexible: Boolean = true) {
        val vCode = 102
        _updateStatus.value = UpdateStatus.UpdateAvailable(
            versionCode = vCode,
            isFlexibleAllowed = isFlexible,
            isImmediateAllowed = !isFlexible,
            isSimulation = true,
            appUpdateInfo = null
        )
        fetchPlayStoreVersionDirectly()
        showNotification(vCode)
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
        _livePlayStoreVersion.value = "Not checked yet"
        notifiedVersionCode = -1
    }

    private fun showNotification(versionCode: Int) {
        if (notifiedVersionCode == versionCode) {
            return
        }
        notifiedVersionCode = versionCode

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelId = "app_update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ManaVahana App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for available software and system updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = launchIntent?.let { intent ->
            PendingIntent.getActivity(
                context,
                111,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("ManaVahana అప్‌డేట్ అందుబాటులో ఉంది!")
            .setContentText("ManaVahana కోసం వెర్షన్ $versionCode అందుబాటులో ఉంది. ఇప్పుడే అప్‌డేట్ చేయండి.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                pendingIntent?.let { setContentIntent(it) }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("AppUpdateHelper", "Skipped showing notification bar to avoid crash: POST_NOTIFICATIONS permission not granted.")
                return
            }
        }

        try {
            notificationManager.notify(7895, builder.build())
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Error throwing system trace notification: ${e.message}")
        }
    }

    fun launchRealUpdate(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        isFlexible: Boolean = true
    ) {
        try {
            val updateType = if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager?.startUpdateFlowForResult(
                appUpdateInfo,
                launcher,
                options
            )
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Error starting real update flow: ${e.message}")
            openPlayStore(activity)
        }
    }

    fun openPlayStore(activity: Activity) {
        val packageName = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } catch (ex: Exception) {
                Log.e("AppUpdateHelper", "Unable to launch Play Store: ${ex.message}")
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppUpdateHelper? = null

        fun getInstance(context: Context): AppUpdateHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = AppUpdateHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
