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
import kotlinx.coroutines.flow.firstOrNull
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

    fun setLivePlayStoreVersion(version: String) {
        _livePlayStoreVersion.value = version
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

    private fun getPrefs() = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    private fun isAlreadyNotified(versionName: String): Boolean {
        if (versionName.isEmpty() || versionName == "Not checked yet" || versionName == "Retrieving...") return false
        return getPrefs().getString("last_notified_version_name", "") == versionName
    }

    private fun markAsNotified(versionName: String) {
        if (versionName.isEmpty() || versionName == "Not checked yet" || versionName == "Retrieving...") return
        getPrefs().edit().putString("last_notified_version_name", versionName).apply()
    }

    fun fetchPlayStoreVersionDirectly() {
        _livePlayStoreVersion.value = "Retrieving..."
        scope.launch {
            val app = context.applicationContext as? com.manavahana.ManaVahanaApplication
            val overriddenPlayStore = app?.userPreferencesRepository?.overriddenPlayStoreVersion?.firstOrNull()
            if (!overriddenPlayStore.isNullOrBlank()) {
                _livePlayStoreVersion.value = overriddenPlayStore
                return@launch
            }

            val packageName = context.packageName
            val version = PlayStoreVersionFetcher.fetchVersion(packageName)
            if (version != null) {
                _livePlayStoreVersion.value = version
            } else {
                // If not found in Play Store (not published yet), default to current installed version name to avoid false update indications
                val packageInfo = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
                val installedVersion = packageInfo?.versionName ?: "1.0"
                _livePlayStoreVersion.value = installedVersion
            }
        }
    }

    fun checkForUpdates(forceNotification: Boolean = false) {
        _updateStatus.value = UpdateStatus.Checking
        scope.launch {
            val app = context.applicationContext as? com.manavahana.ManaVahanaApplication
            val overriddenPlayStore = app?.userPreferencesRepository?.overriddenPlayStoreVersion?.firstOrNull()
            val overriddenApp = app?.userPreferencesRepository?.overriddenAppVersion?.firstOrNull()

            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
            val installedVersion = packageInfo?.versionName ?: "1.0"

            if (!overriddenPlayStore.isNullOrBlank()) {
                _livePlayStoreVersion.value = overriddenPlayStore
                val currentVersion = if (!overriddenApp.isNullOrBlank()) {
                    overriddenApp
                } else {
                    PlayStoreVersionFetcher.getLowerVersion(overriddenPlayStore)
                }

                if (isNewerVersion(currentVersion, overriddenPlayStore)) {
                    val vCode = 102
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        versionCode = vCode,
                        isFlexibleAllowed = true,
                        isImmediateAllowed = false,
                        isSimulation = true,
                        appUpdateInfo = null
                    )
                    showNotification(vCode, force = forceNotification)
                    return@launch
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate
                    return@launch
                }
            }

            // Fallback to Scraping Play Store
            val latestPlayStore = PlayStoreVersionFetcher.fetchVersion(context.packageName)
            if (latestPlayStore != null) {
                _livePlayStoreVersion.value = latestPlayStore
                val currentVersion = if (!overriddenApp.isNullOrBlank()) {
                    overriddenApp
                } else {
                    PlayStoreVersionFetcher.getLowerVersion(latestPlayStore)
                }

                if (isNewerVersion(currentVersion, latestPlayStore)) {
                    val vCode = 102
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        versionCode = vCode,
                        isFlexibleAllowed = true,
                        isImmediateAllowed = false,
                        isSimulation = true,
                        appUpdateInfo = null
                    )
                    showNotification(vCode, force = forceNotification)
                    return@launch
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate
                    return@launch
                }
            }

            val currentVersion = if (!overriddenApp.isNullOrBlank()) overriddenApp else installedVersion

            // Check using real Play Update manager if available
            if (appUpdateManager == null) {
                _updateStatus.value = UpdateStatus.Error("Google Play Services not available")
                return@launch
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
                        showNotification(vCode, force = forceNotification)
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
        _livePlayStoreVersion.value = "1.6"
        showNotification(vCode, force = true)
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
        _livePlayStoreVersion.value = "Not checked yet"
        notifiedVersionCode = -1
        getPrefs().edit().remove("last_notified_version_name").apply()
    }

    private fun showNotification(versionCode: Int, force: Boolean = false) {
        val versionStr = _livePlayStoreVersion.value
        if (!force && isAlreadyNotified(versionStr)) {
            Log.d("AppUpdateHelper", "Already notified for version $versionStr, skipping notification.")
            return
        }
        if (!force) {
            markAsNotified(versionStr)
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

        scope.launch {
            val app = context.applicationContext as? com.manavahana.ManaVahanaApplication
            val langCodeListened = app?.userPreferencesRepository?.selectedLanguage?.firstOrNull()
            val langCode = if (langCodeListened.isNullOrEmpty()) "en" else langCodeListened

            val title = Localizer.get("update_available_title", langCode)

            // Suspend and wait for the live Play Store version if it's currently fetching
            var versionStr = _livePlayStoreVersion.value
            if (versionStr == "Retrieving..." || versionStr == "Not checked yet") {
                val fetched = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    _livePlayStoreVersion.firstOrNull { it != "Retrieving..." && it != "Not checked yet" }
                }
                if (fetched != null) {
                    versionStr = fetched
                }
            }
            if (versionStr == "Retrieving..." || versionStr == "Not checked yet" || versionStr.isEmpty()) {
                versionStr = "1.6"
            }

            val desc = Localizer.get("update_available_desc", langCode).replace("%1\$s", versionStr)

            val largeIconBitmap = try {
                android.graphics.BitmapFactory.decodeResource(context.resources, com.manavahana.R.mipmap.ic_launcher)
            } catch (e: Exception) {
                null
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .apply {
                    largeIconBitmap?.let { setLargeIcon(it) }
                }
                .setContentTitle(title)
                .setContentText(desc)
                .setStyle(NotificationCompat.BigTextStyle().bigText(desc))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .apply {
                    pendingIntent?.let { setContentIntent(it) }
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("AppUpdateHelper", "Skipped showing notification bar to avoid crash: POST_NOTIFICATIONS permission not granted.")
                    return@launch
                }
            }

            try {
                notificationManager.notify(7895, builder.build())
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "Error throwing system trace notification: ${e.message}")
            }
        }
    }

    fun launchRealUpdate(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        isFlexible: Boolean = true
    ) {
        if (appUpdateManager == null) {
            openPlayStore(activity)
            return
        }
        val updateType = if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Failed to start in-app update flow: ${e.message}")
            openPlayStore(activity)
        }
    }

    fun openPlayStore(activity: Activity) {
        val packageName = "com.lochan.ManaVahana"
        try {
            android.widget.Toast.makeText(
                activity,
                "Redirecting to Google Play Store...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            } catch (anfe: Exception) {
                android.widget.Toast.makeText(
                    activity,
                    "Google Play Store could not be opened.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
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
