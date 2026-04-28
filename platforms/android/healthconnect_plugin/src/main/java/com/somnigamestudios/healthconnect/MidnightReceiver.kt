package com.somnigamestudios.healthconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_DATE_CHANGED) {
            StepSensorManager(context).captureNewDayBaseline()
        }
    }
}
