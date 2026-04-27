package com.somnigamestudios.healthconnect

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

class StepSensorManager(
    private val activity: Activity,
    private val godotAndroidPlugin: HealthConnectPlugin,
    private val tag: String
) : SensorEventListener {

    private val sensorManager: SensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val prefs: SharedPreferences = activity.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    private var baseline: Int = prefs.getInt("baseline", 0)
    private var currentSteps: Int = 0

    init {
        // Detect if the device has rebooted since the last session
        val lastBootTime = prefs.getLong("boot_time", 0L)
        val currentBootTime = SystemClock.elapsedRealtime()

        if (currentBootTime < lastBootTime) {
            // Device rebooted, reset baseline
            baseline = 0
            prefs.edit().putInt("baseline", baseline).apply()
        }

        // Save the current boot time for next session comparison
        prefs.edit().putLong("boot_time", currentBootTime).apply()
    }

    fun startListening() {
        if (stepDetector != null) {
            sensorManager.registerListener(
                this,
                stepDetector,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        if (stepCounter != null) {
            sensorManager.registerListener(
                this,
                stepCounter,
                SensorManager.SENSOR_DELAY_NORMAL,
                0
            )
        }
        godotAndroidPlugin.sendSignal("connected")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        godotAndroidPlugin.sendSignal("disconnected")
    }

    fun resetBaseline() {
        baseline += currentSteps
        currentSteps = 0
        prefs.edit().putInt("baseline", baseline).apply()
    }

    fun getCurrentSteps(): Int = currentSteps


    fun getStepsSinceLastCheck(): Int {
        val delta = currentSteps
        baseline += delta
        currentSteps = 0
        prefs.edit().putInt("baseline", baseline).apply()
        return delta
    }

    fun getTotalSteps(): Int {
        return baseline + currentSteps
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                activity.runOnUiThread {
                    godotAndroidPlugin.sendSignal("step_detected")
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val total = event.values[0].toInt()

                if (baseline == 0)
                    baseline = total
                else if (total < baseline)
                    baseline = 0

                currentSteps = total - baseline

                activity.runOnUiThread {godotAndroidPlugin.sendSignal("step_count_updated", currentSteps)}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    // In StepSensorManager.kt (additions)

    private data class Snap(val t: Long, val boot: Long, val total: Int)

    private fun bootEpochMillis(): Long {
        val now = System.currentTimeMillis()
        val up = android.os.SystemClock.elapsedRealtime()
        return now - up
    }

    // --- Simple ring buffer in SharedPreferences (capacity 6 to be safe) ---
    private val SNAP_CAP = 6 // 4 scheduled + a spare or two
    private fun addSnapshot(totalSinceBoot: Int) {
        val p = prefs
        val size = (p.getInt("ss_size", 0)).coerceIn(0, SNAP_CAP)
        val idx = p.getInt("ss_head", -1).let { (it + 1 + SNAP_CAP) % SNAP_CAP }
        val edit = p.edit()
        edit.putLong("ss_${idx}_t", System.currentTimeMillis())
        edit.putLong("ss_${idx}_boot", bootEpochMillis())
        edit.putInt("ss_${idx}_total", totalSinceBoot)
        edit.putInt("ss_head", idx)
        edit.putInt("ss_size", if (size < SNAP_CAP) size + 1 else SNAP_CAP)
        edit.apply()
    }

    private fun loadRecentSnapshots(): List<Snap> {
        val p = prefs
        val size = (p.getInt("ss_size", 0)).coerceIn(0, SNAP_CAP)
        if (size == 0) return emptyList()
        val head = p.getInt("ss_head", -1)
        val out = ArrayList<Snap>(size)
        for (i in 0 until size) {
            val idx = (head - i + SNAP_CAP) % SNAP_CAP
            val t = p.getLong("ss_${idx}_t", 0L)
            val b = p.getLong("ss_${idx}_boot", 0L)
            val tot = p.getInt("ss_${idx}_total", -1)
            if (t > 0 && tot >= 0) out.add(Snap(t, b, tot))
        }
        // Return newest-first; caller can reverse if needed
        return out
    }

    // --- Quick one-shot read of TYPE_STEP_COUNTER (no long-lived listener) ---
    private fun readCounterNowOnce(timeoutMs: Long = 750L): Pair<Long, Int>? {
        val sm = sensorManager
        val sensor = stepCounter ?: return null
        val latch = java.util.concurrent.CountDownLatch(1)
        var captured: Int? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    captured = e.values[0].toInt()
                    latch.countDown()
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        try { latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS) }
        finally { sm.unregisterListener(listener) }

        val total = captured ?: return null
        return bootEpochMillis() to total
    }

    // Call this when you want to snapshot “now” (used by worker & app-open fallback)
    fun snapshotNow() {
        val now = readCounterNowOnce() ?: return
        addSnapshot(now.second)
    }

    // --- Approx last-24h using last 4 snapshots + “now” ---
    fun computeApproxStepsLast24h(): Int {
        val nowPair = readCounterNowOnce() ?: return 0
        val nowSnap = Snap(System.currentTimeMillis(), nowPair.first, nowPair.second)
        val snaps = loadRecentSnapshots().toMutableList() // newest-first
        snaps.add(0, nowSnap) // include “now” as the newest point

        // We only need up to 5 points (now + last 4)
        val take = snaps.take(5).sortedBy { it.t } // sort by time ascending

        // Sum deltas per contiguous boot segment
        var sum = 0
        var segStart: Snap? = null
        var last: Snap? = null
        for (s in take) {
            if (segStart == null) { segStart = s; last = s; continue }
            if (s.boot != last!!.boot) {
                // close previous boot segment
                sum += (last!!.total - segStart.total).coerceAtLeast(0)
                // start new segment
                segStart = s
            }
            last = s
        }
        if (segStart != null && last != null) {
            sum += (last!!.total - segStart.total).coerceAtLeast(0)
        }
        return sum
    }
}