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
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val versionCode: Int,
        val versionName: String = "",
        val isFlexibleAllowed: Boolean = true,
        val isImmediateAllowed: Boolean = false,
        val isSimulation: Boolean = false,
        val appUpdateInfo: AppUpdateInfo? = null
    ) : UpdateStatus()
    data class Downloading(val bytesDownloaded: Long = 0L, val totalBytes: Long = 0L) : UpdateStatus()
    data class UpdateDownloaded(
        val versionName: String = "",
        val appUpdateInfo: AppUpdateInfo? = null,
        val isSimulation: Boolean = false
    ) : UpdateStatus()
    object Installing : UpdateStatus()
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
    private var cachedAppUpdateInfo: AppUpdateInfo? = null
    private var isListenerRegistered: Boolean = false

    private val appUpdateManager: AppUpdateManager? = try {
        AppUpdateManagerFactory.create(context)
    } catch (e: Exception) {
        Log.e("AppUpdateHelper", "Failed to create AppUpdateManager: ${e.message}")
        null
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                _updateStatus.value = UpdateStatus.Downloading(bytesDownloaded, totalBytes)
            }
            InstallStatus.DOWNLOADED -> {
                _updateStatus.value = UpdateStatus.UpdateDownloaded(appUpdateInfo = cachedAppUpdateInfo, isSimulation = false)
            }
            InstallStatus.INSTALLING -> {
                _updateStatus.value = UpdateStatus.Installing
            }
            InstallStatus.INSTALLED -> {
                _updateStatus.value = UpdateStatus.UpToDate
                unregisterUpdateListener()
            }
            InstallStatus.FAILED -> {
                _updateStatus.value = UpdateStatus.Error("Update installation failed (code ${state.installErrorCode()})")
                unregisterUpdateListener()
            }
            InstallStatus.CANCELED -> {
                _updateStatus.value = UpdateStatus.Idle
                unregisterUpdateListener()
            }
            else -> {}
        }
    }

    fun registerUpdateListener() {
        if (!isListenerRegistered && appUpdateManager != null) {
            try {
                appUpdateManager.registerListener(installStateUpdatedListener)
                isListenerRegistered = true
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "Failed to register install listener: ${e.message}")
            }
        }
    }

    fun unregisterUpdateListener() {
        if (isListenerRegistered && appUpdateManager != null) {
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener)
                isListenerRegistered = false
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "Failed to unregister install listener: ${e.message}")
            }
        }
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

    private fun isAlreadyNotified(versionCode: Int, versionName: String): Boolean {
        if (versionName.isBlank() || versionName == "Not checked yet" || versionName == "Retrieving...") return false
        val prefs = getPrefs()
        val lastNotifiedName = prefs.getString("last_notified_version_name", "")
        val lastNotifiedCode = prefs.getInt("last_notified_version_code", -1)
        return (lastNotifiedCode == versionCode && versionCode > 0) || (lastNotifiedName == versionName)
    }

    private fun markAsNotified(versionCode: Int, versionName: String) {
        if (versionName.isBlank() || versionName == "Not checked yet" || versionName == "Retrieving...") return
        getPrefs().edit()
            .putString("last_notified_version_name", versionName)
            .putInt("last_notified_version_code", versionCode)
            .apply()
    }

    fun fetchPlayStoreVersionDirectly() {
        _livePlayStoreVersion.value = "Checking Play Store..."
        scope.launch {
            val packageName = context.packageName
            val version = PlayStoreVersionFetcher.fetchVersion(packageName)
            if (!version.isNullOrBlank()) {
                _livePlayStoreVersion.value = version
            } else {
                var playCoreVersion: String? = null
                if (appUpdateManager != null) {
                    try {
                        val info = withContext(Dispatchers.IO) {
                            com.google.android.gms.tasks.Tasks.await(appUpdateManager.appUpdateInfo)
                        }
                        if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                            playCoreVersion = "v${info.availableVersionCode()}"
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                _livePlayStoreVersion.value = playCoreVersion ?: "Published version not detected"
            }
        }
    }

    fun checkForUpdates(forceNotification: Boolean = false) {
        _updateStatus.value = UpdateStatus.Checking
        scope.launch {
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }
            val installedVersion = packageInfo?.versionName ?: "1.0"
            val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt() ?: 1
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode ?: 1
            }

            // 1. Query Google Play Core In-App Update API
            if (appUpdateManager != null) {
                try {
                    registerUpdateListener()
                    val appUpdateInfo = withContext(Dispatchers.IO) {
                        com.google.android.gms.tasks.Tasks.await(appUpdateManager.appUpdateInfo)
                    }
                    cachedAppUpdateInfo = appUpdateInfo

                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        val vName = _livePlayStoreVersion.value.takeIf { it.isNotBlank() && it != "Checking Play Store..." && it != "Published version not detected" } ?: "v${appUpdateInfo.availableVersionCode()}"
                        _updateStatus.value = UpdateStatus.UpdateDownloaded(
                            versionName = vName,
                            appUpdateInfo = appUpdateInfo,
                            isSimulation = false
                        )
                        return@launch
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        _updateStatus.value = UpdateStatus.Downloading(0L, 0L)
                        return@launch
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        val isFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        val isImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val vCode = appUpdateInfo.availableVersionCode()
                        
                        // Fetch exact published version name from Play Store web listing
                        val scrapedVersion = PlayStoreVersionFetcher.fetchVersion(context.packageName)
                        val targetVersionName = if (!scrapedVersion.isNullOrBlank() && PlayStoreVersionFetcher.isNewerVersion(installedVersion, scrapedVersion)) {
                            scrapedVersion
                        } else {
                            "v$vCode"
                        }
                        _livePlayStoreVersion.value = targetVersionName

                        _updateStatus.value = UpdateStatus.UpdateAvailable(
                            versionCode = vCode,
                            versionName = targetVersionName,
                            isFlexibleAllowed = isFlexible,
                            isImmediateAllowed = isImmediate,
                            isSimulation = false,
                            appUpdateInfo = appUpdateInfo
                        )
                        showNotification(versionCode = vCode, versionName = targetVersionName, force = forceNotification)
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w("AppUpdateHelper", "Play Core In-App Update API query failed or unreleased: ${e.message}")
                }
            }

            // 2. Query Google Play Store Public Web metadata
            val latestPlayStore = PlayStoreVersionFetcher.fetchVersion(context.packageName)
            if (!latestPlayStore.isNullOrBlank()) {
                _livePlayStoreVersion.value = latestPlayStore
                if (PlayStoreVersionFetcher.isNewerVersion(installedVersion, latestPlayStore)) {
                    val computedCode = installedCode + 1
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        versionCode = computedCode,
                        versionName = latestPlayStore,
                        isFlexibleAllowed = true,
                        isImmediateAllowed = false,
                        isSimulation = false,
                        appUpdateInfo = null
                    )
                    showNotification(versionCode = computedCode, versionName = latestPlayStore, force = forceNotification)
                    return@launch
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate
                    return@launch
                }
            } else {
                if (_livePlayStoreVersion.value == "Checking Play Store..." || _livePlayStoreVersion.value == "Not checked yet") {
                    _livePlayStoreVersion.value = "Published version not detected"
                }
            }

            // 3. Up to date
            _updateStatus.value = UpdateStatus.UpToDate
        }
    }

    fun completeUpdate() {
        if (appUpdateManager != null) {
            try {
                _updateStatus.value = UpdateStatus.Installing
                appUpdateManager.completeUpdate()
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "Failed to complete update: ${e.message}")
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Failed to complete update")
            }
        } else {
            scope.launch {
                _updateStatus.value = UpdateStatus.Installing
                delay(1000)
                _updateStatus.value = UpdateStatus.UpToDate
            }
        }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
        _livePlayStoreVersion.value = "Not checked yet"
        notifiedVersionCode = -1
        getPrefs().edit()
            .remove("last_notified_version_name")
            .remove("last_notified_version_code")
            .apply()
    }

    private fun showNotification(versionCode: Int, versionName: String, force: Boolean = false) {
        if (!force && isAlreadyNotified(versionCode, versionName)) {
            Log.d("AppUpdateHelper", "Already notified for version $versionName (code $versionCode), skipping notification.")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelId = "app_update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ManaVahana App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for available software and system updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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

        // Store redirection intent for Action button
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val storePendingIntent = PendingIntent.getActivity(
            context,
            112,
            storeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scope.launch {
            val app = context.applicationContext as? com.manavahana.ManaVahanaApplication
            val langCodeListened = app?.userPreferencesRepository?.selectedLanguage?.firstOrNull()
            val langCode = if (langCodeListened.isNullOrEmpty()) "en" else langCodeListened

            val title = Localizer.get("update_available_title", langCode)
            val desc = Localizer.get("update_available_desc", langCode).replace("%1\$s", versionName)
            val actionTitle = if (langCode == "te") "ఇప్పుడే అప్‌డేట్ చేయండి" else "Update Now"

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
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .apply {
                    pendingIntent?.let { setContentIntent(it) }
                }
                .addAction(android.R.drawable.stat_sys_download, actionTitle, storePendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("AppUpdateHelper", "Skipped showing notification: POST_NOTIFICATIONS permission not granted. Will notify once granted.")
                    return@launch
                }
            }

            try {
                notificationManager.notify(7895, builder.build())
                notifiedVersionCode = versionCode
                markAsNotified(versionCode, versionName)
                Log.d("AppUpdateHelper", "Successfully posted update notification for version $versionName (code $versionCode)")
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "Error publishing system update notification: ${e.message}")
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
            registerUpdateListener()
            _updateStatus.value = UpdateStatus.Downloading(0L, 0L)
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Failed to start in-app update flow: ${e.message}")
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Failed to start update flow")
            openPlayStore(activity)
        }
    }

    fun openPlayStore(activity: Activity) {
        val packageName = activity.packageName
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
