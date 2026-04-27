package com.somnigamestudios.healthconnect

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.Manifest
import android.content.pm.PackageManager
import android.nfc.Tag
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager(private val activity: Activity, private val godotAndroidPlugin: HealthConnectPlugin, private val tag: String) {
    private lateinit var requestPermissions: ActivityResultLauncher<Set<String>>
    private var hasPermissions: Boolean = false
    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun initializePermissionLauncher() {
        if (activity is FragmentActivity) {
            val fragmentActivity = activity
            requestPermissions = fragmentActivity.registerForActivityResult(
                PermissionController.createRequestPermissionResultContract()
            ) { granted ->
                if (granted.containsAll(PERMISSIONS)) {
                    hasPermissions = true
                    Log.i(tag, "Permissions successfully granted.")
                } else {
                    hasPermissions = false
                    Log.w(tag, "Health Connect permissions not fully granted.")
                }
            }
            Log.d(tag, "Permission launcher initialized.")
        } else {
            Log.e(
                tag,
                "Activity is not a FragmentActivity. Cannot initialize permission launcher."
            )
        }
    }

    fun requestUserPermissions() {
        if (::requestPermissions.isInitialized) {
            Log.i(tag, "Requesting user permissions.")
            requestPermissions.launch(PERMISSIONS)
        } else {
            Log.e(
                tag,
                "Permission launcher is not initialized. Cannot request permissions."
            )
        }
    }

    fun arePermissionsGrantedSync(healthConnectClient: HealthConnectClient): Boolean {
        var grantedPermissions:Set<String>? = null
        runBlocking { grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions() }
        hasPermissions = grantedPermissions?.containsAll(PERMISSIONS) == true
        Log.i(tag, "Returning cached permissions state: $hasPermissions")
        return hasPermissions
    }


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
                godotAndroidPlugin.startStepSensors()
                Log.i(tag, "Activity Recognition permission granted")
            } else {
                Log.i(tag, "Activity Recognition permission denied")
            }
            godotAndroidPlugin.sendSignal("activity_permission_result", granted)
        }
    }
}