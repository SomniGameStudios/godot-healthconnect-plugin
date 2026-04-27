package com.somnigamestudios.healthconnect

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.Toast
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class HealthConnectPlugin(godot: Godot) : GodotPlugin(godot) {

    private val tag = "godot"
    private var permissionsManager: PermissionsManager
    private var healthConnectManager: HealthConnectManager
    private var stepSensorManager: StepSensorManager

    override fun getPluginName() = "HealthConnectPlugin"

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        val signals: MutableSet<SignalInfo> = mutableSetOf()
        signals.add(SignalInfo("connected"))
        signals.add(SignalInfo("disconnected"))
        signals.add(SignalInfo("today_steps", Integer::class.java))

        signals.add(SignalInfo("step_detected"))
        signals.add(SignalInfo("step_count_updated", Integer::class.java))

        signals.add(SignalInfo("activity_permission_result", Boolean::class.javaObjectType))

        return signals
    }

    override fun onMainCreate(activity: Activity?): View? {
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            Log.e(tag, "Uncaught Exception! ${exception.message}")
        }
        return super.onMainCreate(activity)
    }

    override fun onMainRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    init {
        val activityInstance = activity?: throw IllegalStateException("Activity is null")
        permissionsManager = PermissionsManager(activityInstance, this, tag)
        healthConnectManager = HealthConnectManager(activityInstance, permissionsManager, this, tag)
        stepSensorManager = StepSensorManager(activityInstance, this, tag)
        permissionsManager.initializePermissionLauncher()
        //StepSnapshotScheduler.schedule(activityInstance)
    }

    @UsedByGodot
    fun checkHealthConnectInstalled(): Boolean {
        return healthConnectManager.checkHealthConnectInstalled()
    }

    @UsedByGodot
    fun checkHealthConnectUpdated(): Boolean {
        return healthConnectManager.checkHealthConnectUpdated()
    }

    @UsedByGodot
    fun promptHealthConnectInstall() {
        healthConnectManager.promptHealthConnectInstall()
    }

    @UsedByGodot
    fun promptHealthConnectUpdate() {
        healthConnectManager.promptHealthConnectUpdate()
    }

    @UsedByGodot
    fun requestHealthConnectPermissions() {
        healthConnectManager.requestHealthConnectPermissions()
    }

    @UsedByGodot
    fun arePermissionsGrantedSync(): Boolean {
        return healthConnectManager.arePermissionsGranted()
    }

    // Activity recognition permissions are needed for Step Sensors to work
    @UsedByGodot
    fun requestActivityPermission() {
        permissionsManager.requestActivityRecognitionPermission()
    }

    @UsedByGodot
    fun isActivityPermissionGranted(): Boolean {
        return permissionsManager.isActivityRecognitionGranted()
    }

    @UsedByGodot
    fun getTodaySteps(): Int {
        return healthConnectManager.getTodaySteps()
    }

    @UsedByGodot
    fun getYesterdaySteps(): Int {
        return healthConnectManager.getYesterdaySteps()
    }

    @UsedByGodot
    fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            Log.v(tag, "Message from Godot: $msg")
        }
    }

    @UsedByGodot
    fun sendSignal(name: String) {
        emitSignal(name)
    }
    @UsedByGodot
    fun sendSignal(name: String, param: Any) {
        emitSignal(name, param)
    }

    @UsedByGodot
    fun startStepSensors() {
        stepSensorManager.startListening()
    }

    @UsedByGodot
    fun stopStepSensors() {
        stepSensorManager.stopListening()
    }

    @UsedByGodot
    fun resetStepCounterBaseline() {
        stepSensorManager.resetBaseline()
    }

    @UsedByGodot
    fun getCurrentSteps(): Int {
        return stepSensorManager.getCurrentSteps()
    }

    @UsedByGodot
    fun getStepsSinceLastCheck(): Int {
        return stepSensorManager.getStepsSinceLastCheck()
    }

    @UsedByGodot
    fun getApproxStepsLast24h(): Int {
        return stepSensorManager.computeApproxStepsLast24h()
    }

}
