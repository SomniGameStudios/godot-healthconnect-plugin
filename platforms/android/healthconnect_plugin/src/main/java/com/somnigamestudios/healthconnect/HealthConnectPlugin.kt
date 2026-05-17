package com.somnigamestudios.healthconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import org.json.JSONObject

class HealthConnectPlugin(godot: Godot) : GodotPlugin(godot) {

    private val tag = "godot"
    private var permissionsManager: PermissionsManager
    private var stepSensorManager: StepSensorManager
    private var midnightReceiver: MidnightReceiver

    // Cached values — updated on query completion, just like iOS
    @Volatile private var cachedTodaySteps: Int = 0
    @Volatile private var cachedTotalSteps: Int = 0
    @Volatile private var cachedPeriodSteps: Map<String, Int> = emptyMap()

    override fun getPluginName() = "HealthConnectPlugin"

    override fun getPluginSignals(): MutableSet<SignalInfo> = mutableSetOf(
        // iOS HealthKit parity signals
        SignalInfo("permission_result", Boolean::class.javaObjectType),
        SignalInfo("today_steps_ready", Integer::class.java),
        SignalInfo("total_steps_ready", Integer::class.java),
        SignalInfo("period_steps_ready", org.godotengine.godot.Dictionary::class.java),
        SignalInfo("steps_updated", Integer::class.java),

        // Pedometer (CMPedometer equivalent) signals
        SignalInfo("pedometer_steps_updated", Integer::class.java),
        SignalInfo("pedometer_error", String::class.java),

        // Low-level sensor signals (kept for compatibility)
        SignalInfo("step_detected"),
        SignalInfo("step_count_updated", Integer::class.java),
        SignalInfo("sensor_connected"),
        SignalInfo("sensor_disconnected"),
    )

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
        val activityInstance = activity ?: throw IllegalStateException("Activity is null")
        permissionsManager = PermissionsManager(activityInstance, this, tag)
        stepSensorManager = StepSensorManager(activityInstance, this, tag)
        midnightReceiver = MidnightReceiver()

        // Register ACTION_DATE_CHANGED receiver for exact midnight baseline capture
        val filter = IntentFilter(Intent.ACTION_DATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityInstance.registerReceiver(midnightReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            activityInstance.registerReceiver(midnightReceiver, filter)
        }

        // Ensure the stored baseline is for today (handles app launch on a new day)
        stepSensorManager.ensureBaselineIsCurrentDay()
    }

    // ---- iOS HealthKit API parity ----

    @UsedByGodot
    fun requestPermission() {
        permissionsManager.requestActivityRecognitionPermission()
    }

    @UsedByGodot
    fun getPermissionStatus(): Int {
        // Mirror iOS HKAuthorizationStatus: 0=not_determined, 1=denied, 2=authorized
        return when {
            permissionsManager.isActivityRecognitionGranted() -> 2
            else -> 0
        }
    }

    @UsedByGodot
    fun isHealthDataAvailable(): Boolean = true

    @UsedByGodot
    fun openSettings() {
        val activityInstance = activity ?: return
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activityInstance.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityInstance.startActivity(intent)
    }

    @UsedByGodot
    fun runTodayStepsQuery() {
        val steps = stepSensorManager.getTodaySteps()
        cachedTodaySteps = steps
        emitSignal("today_steps_ready", steps)
    }

    @UsedByGodot
    fun runTotalStepsQuery() {
        val steps = stepSensorManager.getTotalSteps()
        cachedTotalSteps = steps
        emitSignal("total_steps_ready", steps)
    }

    @UsedByGodot
    fun runPeriodStepsQuery(days: Int) {
        val periodMap = stepSensorManager.getPeriodStepsDict(days)
        cachedPeriodSteps = periodMap

        val dict = org.godotengine.godot.Dictionary()
        for ((k, v) in periodMap) dict[k] = v
        emitSignal("period_steps_ready", dict)
    }

    @UsedByGodot
    fun getTodaySteps(): Int = cachedTodaySteps

    @UsedByGodot
    fun getTotalSteps(): Int = cachedTotalSteps

    @UsedByGodot
    fun getPeriodStepsDict(): org.godotengine.godot.Dictionary {
        val dict = org.godotengine.godot.Dictionary()
        for ((k, v) in cachedPeriodSteps) dict[k] = v
        return dict
    }

    // ---- Step observer (HKObserverQuery equivalent) ----

    @UsedByGodot
    fun startStepObserver() {
        stepSensorManager.startListening()
    }

    @UsedByGodot
    fun stopStepObserver() {
        stepSensorManager.stopListening()
    }

    // ---- Pedometer (CMPedometer equivalent) ----

    @UsedByGodot
    fun isPedometerAvailable(): Boolean = true

    @UsedByGodot
    fun getPedometerPermissionStatus(): Int = getPermissionStatus()

    @UsedByGodot
    fun startPedometerObserver() {
        stepSensorManager.startListening()
    }

    @UsedByGodot
    fun stopPedometerObserver() {
        stepSensorManager.stopListening()
    }

    @UsedByGodot
    fun getLivePedometerSteps(): Int = stepSensorManager.getLiveSessionSteps()

    // ---- Permission ----

    @UsedByGodot
    fun requestActivityPermission() {
        permissionsManager.requestActivityRecognitionPermission()
    }

    @UsedByGodot
    fun isActivityPermissionGranted(): Boolean = permissionsManager.isActivityRecognitionGranted()

    // ---- Internal signal bridge (called by StepSensorManager) ----

    fun sendSignal(name: String) = emitSignal(name)
    fun sendSignal(name: String, param: Any) = emitSignal(name, param)
}
