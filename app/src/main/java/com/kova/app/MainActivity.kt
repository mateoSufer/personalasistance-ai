package com.kova.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kova.app.alarm.AlarmScheduler
import com.kova.app.domain.detector.DistractionDetector
import com.kova.app.domain.detector.SensorFusionEngine
import com.kova.app.navigation.AppNavigator
import com.kova.app.service.KovaMonitorService
import com.kova.app.ui.theme.KovaTheme

class MainActivity : ComponentActivity() {

    private lateinit var detector: DistractionDetector
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var fusionEngine: SensorFusionEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detector = DistractionDetector(this)
        alarmScheduler = AlarmScheduler(this)
        fusionEngine = SensorFusionEngine(this)

        val startScreen = intent?.getStringExtra("screen") ?: "welcome"

        enableEdgeToEdge()
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            KovaTheme {
                AppNavigator(
                    detector = detector,
                    alarmScheduler = alarmScheduler,
                    fusionEngine = fusionEngine,
                    initialScreen = startScreen,
                    onStartService = { name, goal -> startKovaService(name, goal) },
                    onOpenPermissions = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
    }

    private fun startKovaService(name: String, goal: String) {
        val intent = Intent(this, KovaMonitorService::class.java).apply {
            putExtra(KovaMonitorService.EXTRA_NAME, name)
            putExtra(KovaMonitorService.EXTRA_GOAL, goal)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
