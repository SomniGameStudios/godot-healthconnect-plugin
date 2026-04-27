package com.somnigamestudios.healthconnect
/*
// StepSnapshotScheduler.kt

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

object StepSnapshotScheduler {
    fun schedule(context: Context) {
        val delay = initialDelayToNext6hBoundary()
        val req = PeriodicWorkRequestBuilder<StepSnapshotWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("step_snapshots_6h")
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "step_snapshots_6h",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    private fun initialDelayToNext6hBoundary(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.HOUR_OF_DAY) % 6 != 0) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        return cal.timeInMillis - System.currentTimeMillis()
    }
}
*/