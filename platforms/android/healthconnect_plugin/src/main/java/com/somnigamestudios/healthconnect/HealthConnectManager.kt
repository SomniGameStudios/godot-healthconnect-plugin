package com.somnigamestudios.healthconnect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(
    private val activity: Activity,
    private val permissionsManager: PermissionsManager,
    private val godotAndroidPlugin: HealthConnectPlugin,
    private val tag: String
) {
    private val providerPackageName = "com.google.android.apps.healthdata"
    private var healthConnectClient: HealthConnectClient? = null
    private var cachedStepsToday: Int = -1
    private var cachedStepsYesterday: Int = -1

    init {
        initializeHealthConnectClient()
    }

    private fun initializeHealthConnectClient() {
        val context = activity.applicationContext
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
        } else {
            Log.e(tag, "Health Connect is not available.")
        }
    }

    private fun promptInstallHealthConnect() {
        activity.runOnUiThread {
            val uriString =
                "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = Uri.parse(uriString)
                putExtra("overlay", true)
                putExtra("callerId", activity.applicationContext.packageName)
            }
            activity.startActivity(intent)
        }
    }

    private fun completeHealthConnectCheck(): Boolean {
        if (!isHealthConnectInstalled()) {
            Log.i(tag,"Health Connect is not installed. Please install it to retrieve steps.")
            return false
        }
        if (!isHealthConnectUpdated()) {
            Log.i(tag,"Health Connect needs to be updated. Please update it to retrieve steps.")
            return false
        }
        if (!arePermissionsGranted()) {
            Log.i(tag,"Permissions are not granted. Please grant the necessary permissions to retrieve steps.")
            return false
        }
        return true
    }

    fun checkHealthConnectInstalled(): Boolean {
        val context = activity.applicationContext
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    fun checkHealthConnectUpdated(): Boolean {
        val context = activity.applicationContext
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)
        return status != HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    fun promptHealthConnectInstall() {
        promptInstallHealthConnect()
    }

    fun promptHealthConnectUpdate() {
        promptInstallHealthConnect()
    }

    fun isHealthConnectInstalled(): Boolean {
        val context = activity.applicationContext
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)

        if (status == HealthConnectClient.SDK_AVAILABLE) {
            return true
        }

        Log.w(tag, "Health Connect is not installed or unavailable.")
        if (status == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.i(tag, "Prompting user to install Health Connect.")
            promptInstallHealthConnect()
        }
        return false
    }

    fun isHealthConnectUpdated(): Boolean {
        val context = activity.applicationContext
        val status = HealthConnectClient.getSdkStatus(context, providerPackageName)

        if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.i(tag, "Prompting user to update Health Connect.")
            promptInstallHealthConnect()
            return false
        }
        return true
    }

    fun arePermissionsGranted(): Boolean {
        if (healthConnectClient == null) {
            Log.e(
                tag,
                "HealthConnectClient is not initialized. Cannot check permissions."
            )
            return false
        }
        return permissionsManager.arePermissionsGrantedSync(healthConnectClient!!)
    }

    private fun updateStepCache() {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val yesterdayStart = todayStart.minusSeconds(24 * 60 * 60)

        CoroutineScope(Dispatchers.IO).launch {
            cachedStepsToday = aggregateSteps(todayStart, now).steps
            godotAndroidPlugin.sendSignal("today_steps", cachedStepsToday)
            Log.i(tag, "Cached today's steps: $cachedStepsToday")
        }

        CoroutineScope(Dispatchers.IO).launch {
            cachedStepsYesterday = aggregateSteps(yesterdayStart, todayStart).steps
            Log.i(tag, "Cached yesterday's steps: $cachedStepsYesterday")
        }
    }

    private suspend fun aggregateSteps(startTime: Instant, endTime: Instant): StepResult {
        return try {
            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient!!.aggregate(request)
            StepResult(response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0)
        } catch (e: Exception) {
            Log.e(tag, "Error aggregating steps: ", e)
            StepResult(-1, true)
        }
    }

    fun getTodaySteps(): Int {
        if (!completeHealthConnectCheck()) {
            Log.e(tag, "Health Connect is not ready. Cannot retrieve steps.")
            return -1
        }
        updateStepCache()
        return cachedStepsToday
    }

    fun getYesterdaySteps(): Int {
        if (!completeHealthConnectCheck()) {
            Log.e(tag, "Health Connect is not ready. Cannot retrieve steps.")
            return -1
        }
        updateStepCache()
        return cachedStepsYesterday
    }

    fun requestHealthConnectPermissions() {
        permissionsManager.requestUserPermissions()
    }

    data class StepResult(val steps: Int, val error: Boolean = false)
}