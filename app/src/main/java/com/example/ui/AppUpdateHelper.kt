package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val appUpdateManager = try {
        AppUpdateManagerFactory.create(context)
    } catch (e: Exception) {
        Log.e("AppUpdateHelper", "Failed to create AppUpdateManager: ${e.message}")
        null
    }

    fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking
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
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        versionCode = appUpdateInfo.availableVersionCode(),
                        isFlexibleAllowed = isFlexible,
                        isImmediateAllowed = isImmediate,
                        isSimulation = false,
                        appUpdateInfo = appUpdateInfo
                    )
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
        _updateStatus.value = UpdateStatus.UpdateAvailable(
            versionCode = 102,
            isFlexibleAllowed = isFlexible,
            isImmediateAllowed = !isFlexible,
            isSimulation = true,
            appUpdateInfo = null
        )
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
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
