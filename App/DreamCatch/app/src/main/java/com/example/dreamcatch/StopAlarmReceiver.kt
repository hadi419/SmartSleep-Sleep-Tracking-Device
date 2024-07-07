package com.example.dreamcatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmService::class.java)
        serviceIntent.action = "STOP"
        context?.startService(serviceIntent)
    }
}

