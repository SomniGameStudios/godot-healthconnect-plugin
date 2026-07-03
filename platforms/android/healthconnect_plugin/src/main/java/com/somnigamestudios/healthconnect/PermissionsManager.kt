package com.somnigamestudios.healthconnect

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager(private val activity: Activity, private val godotAndroidPlugin: HealthConnectPlugin, private val tag: String) {

    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Step Sensor Permissions

    fun isActivityRecognitionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestActivityRecognitionPermission() {
        if (!isActivityRecognitionGranted()) {
            prefs.edit().putBoolean(KEY_ACTIVITY_PERMISSION_REQUESTED, true).apply()
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVITY_RECOGNITION_REQUEST_CODE
            )
        }
    }

    /**
     * Resolves a 4-state permission status. Android exposes no direct "permanently denied"
     * query, so denied-permanently is inferred: once a request has been made (persisted flag)
     * and the system no longer asks us to show a rationale, the user has chosen "don't ask again".
     */
    fun getActivityRecognitionStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return STATUS_GRANTED
        if (isActivityRecognitionGranted()) return STATUS_GRANTED
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACTIVITY_RECOGNITION)) return STATUS_DENIED_CAN_ASK
        return if (prefs.getBoolean(KEY_ACTIVITY_PERMISSION_REQUESTED, false)) STATUS_DENIED_PERMANENTLY else STATUS_NOT_REQUESTED
    }

    companion object {
        private const val ACTIVITY_RECOGNITION_REQUEST_CODE = 2025

        private const val PREFS_NAME = "healthconnect_plugin_prefs"
        private const val KEY_ACTIVITY_PERMISSION_REQUESTED = "activity_permission_requested"

        const val STATUS_NOT_REQUESTED = 0
        const val STATUS_DENIED_CAN_ASK = 1
        const val STATUS_DENIED_PERMANENTLY = 2
        const val STATUS_GRANTED = 3
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.i(tag, "Activity Recognition permission granted")
            } else {
                Log.i(tag, "Activity Recognition permission denied")
            }
            godotAndroidPlugin.sendSignal("permission_result", granted)
        }
    }
}