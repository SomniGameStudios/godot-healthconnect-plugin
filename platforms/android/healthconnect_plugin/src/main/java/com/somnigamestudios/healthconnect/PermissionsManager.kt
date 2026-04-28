package com.somnigamestudios.healthconnect

import android.app.Activity
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager(private val activity: Activity, private val godotAndroidPlugin: HealthConnectPlugin, private val tag: String) {

    // Step Sensor Permissions

    fun isActivityRecognitionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestActivityRecognitionPermission() {
        if (!isActivityRecognitionGranted()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVITY_RECOGNITION_REQUEST_CODE
            )
        }
    }

    companion object {
        private const val ACTIVITY_RECOGNITION_REQUEST_CODE = 2025
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                godotAndroidPlugin.startStepObserver()
                Log.i(tag, "Activity Recognition permission granted")
            } else {
                Log.i(tag, "Activity Recognition permission denied")
            }
            godotAndroidPlugin.sendSignal("permission_result", granted)
        }
    }
}