package com.example.dreamcatch

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var isAlarmPlaying = false

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val ACTION_ALARM_STATE = "com.example.dreamcatch.ACTION_ALARM_STATE"
        const val EXTRA_ALARM_STATE = "alarm_state"
    }
    private val handler = Handler()



    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.creepy)
        mediaPlayer.isLooping = true

        val stopAlarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopAlarm()
            }
        }
        val filter = IntentFilter("STOP_ALARM_AUDIO")
        LocalBroadcastManager.getInstance(this).registerReceiver(stopAlarmReceiver, filter)
    }

    private fun broadcastAlarmState() {
        val intent = Intent(ACTION_ALARM_STATE)
        intent.putExtra(EXTRA_ALARM_STATE, isAlarmPlaying)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopAlarm()
        } else {
//            showNotification()
            mediaPlayer.start()
        }
        isAlarmPlaying = true
        return START_NOT_STICKY
    }
    fun stopAlarm() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
            isAlarmPlaying = false

            handler.postDelayed({
                if (isServiceRunning(this)) {
                    stopSelf()
                }
                broadcastAlarmState()
            }, 1000)
        }
    }


    private fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager?.getRunningServices(Int.MAX_VALUE) ?: emptyList()) {
            if (AlarmService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}


