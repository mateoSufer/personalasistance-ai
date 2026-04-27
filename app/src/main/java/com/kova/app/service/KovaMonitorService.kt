package com.kova.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kova.app.domain.detector.DistractionDetector
import com.kova.app.domain.model.UserProfile
import kotlinx.coroutines.*

class KovaMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var detector: DistractionDetector
    private var userProfile = UserProfile()

    companion object {
        const val CHANNEL_ID = "kova_monitor_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_GOAL = "extra_goal"
    }

    override fun onCreate() {
        super.onCreate()
        detector = DistractionDetector(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra(EXTRA_NAME) ?: ""
        val goal = intent?.getStringExtra(EXTRA_GOAL) ?: ""
        userProfile = UserProfile(name = name, goal = goal)
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (true) {
                if (!detector.isPaused && detector.hasPermission() && detector.isDistracted(userProfile)) {
                    sendDistractionAlert(userProfile)
                }
                delay(5000)
            }
        }
    }

    private fun sendDistractionAlert(profile: UserProfile) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val alert = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("${profile.name}, you're getting distracted!")
            .setContentText("Remember your goal: ${profile.goal}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, alert)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Kova is watching")
            .setContentText("I'll alert you if you get distracted.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kova Monitor",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
