package com.somnigamestudios.healthconnect

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StepSensorManager(
    private val context: Context,
    private val plugin: HealthConnectPlugin? = null,
    private val tag: String = "godot"
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("step_sensor_prefs", Context.MODE_PRIVATE)

    // Live session counter (steps taken since startListening() was last called)
    private var liveSessionSteps: Int = 0
    private var liveSessionBase: Int = -1

    // ---- Today's steps helpers ----

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun todayString(): String = dateFormat.format(Date())

    fun startListening() {
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, 0)
        }
        plugin?.sendSignal("connected")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        plugin?.sendSignal("disconnected")
    }

    // Returns steps counted since startListening() was last called
    fun getLiveSessionSteps(): Int = liveSessionSteps

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                // Real-time optimistic update
                liveSessionSteps++
                plugin?.sendSignal("pedometer_steps_updated", liveSessionSteps)
                plugin?.sendSignal("step_detected")
                
                // Optimistically increment the total counter for real-time UI feedback
                val lastKnown = prefs.getInt("last_known_counter", -1)
                if (lastKnown >= 0) {
                    val optimisticTotal = lastKnown + 1
                    prefs.edit().putInt("last_known_counter", optimisticTotal).apply()
                    val todaySteps = computeTodaySteps(optimisticTotal)
                    plugin?.sendSignal("steps_updated", todaySteps)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val total = event.values[0].toInt()
                prefs.edit().putInt("last_known_counter", total).apply()
                
                if (liveSessionBase < 0) liveSessionBase = total
                liveSessionSteps = (total - liveSessionBase).coerceAtLeast(0)
                plugin?.sendSignal("pedometer_steps_updated", liveSessionSteps)
                plugin?.sendSignal("step_count_updated", liveSessionSteps)

                // The absolute truth syncs with the UI here, correcting any optimistic drift
                val todaySteps = computeTodaySteps(total)
                plugin?.sendSignal("steps_updated", todaySteps)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---- Midnight baseline ----

    /**
     * Called by MidnightReceiver at DATE_CHANGED.
     * Saves yesterday's total into history and sets a new midnight baseline.
     */
    fun captureNewDayBaseline() {
        val now = readCounterNowOnce() ?: return
        val counterNow = now.second

        val storedDate = prefs.getString("midnight_date", "") ?: ""
        val storedBaseline = prefs.getInt("midnight_baseline", 0)

        // Save yesterday's total to history only if we have a valid stored date
        if (storedDate.isNotEmpty() && storedDate != todayString()) {
            val yesterdayTotal = (counterNow - storedBaseline).coerceAtLeast(0)
            saveToHistory(storedDate, yesterdayTotal)
        }

        // Set the new midnight baseline
        prefs.edit()
            .putInt("midnight_baseline", counterNow)
            .putString("midnight_date", todayString())
            .apply()
    }

    /**
     * Called on plugin init to handle the case where the device was off over midnight or
     * the app was first launched on a new day.
     */
    fun ensureBaselineIsCurrentDay() {
        val storedDate = prefs.getString("midnight_date", "") ?: ""
        if (storedDate == todayString()) return // Already up to date

        val now = readCounterNowOnce() ?: return

        // Check if device has been running since before midnight
        val bootEpoch = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightEpoch = cal.timeInMillis

        if (storedDate.isNotEmpty()) {
            // Save the previous day's total to history
            val storedBaseline = prefs.getInt("midnight_baseline", 0)
            if (bootEpoch < midnightEpoch) {
                // Device was ON over midnight: previous baseline is valid
                val prevDayTotal = (now.second - storedBaseline).coerceAtLeast(0)
                saveToHistory(storedDate, prevDayTotal)
            }
            // If device rebooted today, we can't recover yesterday's accurate count.
            // Save what we have (0 or partial) to avoid losing the history entry entirely.
        }

        // Determine new midnight baseline
        val newBaseline = if (bootEpoch < midnightEpoch) {
            // Device was running at midnight: reconstruct baseline via elapsedRealtime is not
            // possible without the actual sensor reading at midnight.
            // Best approximation: use the stored counter at midnight (we can't go back in time).
            // We set 0 here when the device rebooted after midnight since counter starts fresh.
            now.second // This gives today_steps = 0 until counter grows from now.
        } else {
            // Device rebooted today (after midnight): counter started fresh.
            now.second // today_steps = counter_now - baseline → as steps accumulate it will grow.
        }

        prefs.edit()
            .putInt("midnight_baseline", newBaseline)
            .putString("midnight_date", todayString())
            .apply()
    }

    private fun computeTodaySteps(currentCounterValue: Int): Int {
        val storedDate = prefs.getString("midnight_date", "") ?: ""
        val storedBaseline = prefs.getInt("midnight_baseline", 0)

        if (storedDate != todayString()) {
            // Baseline is stale — app open triggered ensureBaselineIsCurrentDay which set a new one,
            // but it might not have persisted yet. Return 0 as a safe fallback.
            return 0
        }

        // If device rebooted today, the counter restarted from 0 and the baseline was set to the
        // counter reading right after boot (approx 0). As steps accumulate, today_steps grows.
        return (currentCounterValue - storedBaseline).coerceAtLeast(0)
    }

    fun getTodaySteps(): Int {
        val current = readCounterNowOnce()?.second ?: return 0
        return computeTodaySteps(current)
    }

    fun getTotalSteps(): Int {
        val historyTotal = loadAllHistoryTotal()
        return historyTotal + getTodaySteps()
    }

    fun getPeriodStepsDict(days: Int): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val historyJson = prefs.getString("history_json", "{}") ?: "{}"
        val history = JSONObject(historyJson)

        val cal = Calendar.getInstance()
        val todayStr = todayString()
        result[todayStr] = getTodaySteps()

        for (i in 1 until days) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            result[dateStr] = if (history.has(dateStr)) history.getInt(dateStr) else 0
        }

        return result
    }

    // ---- Internal helpers ----

    private fun saveToHistory(dateStr: String, steps: Int) {
        val historyJson = prefs.getString("history_json", "{}") ?: "{}"
        val obj = JSONObject(historyJson)
        obj.put(dateStr, steps)

        // Prune entries older than 30 days to avoid unbounded growth
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
        val cutoffStr = dateFormat.format(cutoff.time)
        val keys = obj.keys().asSequence().toList()
        for (key in keys) {
            if (key < cutoffStr) obj.remove(key)
        }

        prefs.edit().putString("history_json", obj.toString()).apply()
    }

    private fun loadAllHistoryTotal(): Int {
        val historyJson = prefs.getString("history_json", "{}") ?: "{}"
        val obj = JSONObject(historyJson)
        var total = 0
        val keys = obj.keys()
        while (keys.hasNext()) total += obj.getInt(keys.next())
        return total
    }

    fun readCounterNowOnce(timeoutMs: Long = 1000L): Pair<Long, Int>? {
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
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.flush(listener)
        try { latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS) }
        finally { sensorManager.unregisterListener(listener) }

        if (captured != null) {
            prefs.edit().putInt("last_known_counter", captured!!).apply()
            return System.currentTimeMillis() to captured!!
        }
        
        val lastKnown = prefs.getInt("last_known_counter", -1)
        if (lastKnown >= 0) {
            return System.currentTimeMillis() to lastKnown
        }
        return null
    }
}