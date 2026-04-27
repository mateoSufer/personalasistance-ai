package com.kova.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kova.app.alarm.AlarmScheduler
import com.kova.app.domain.detector.DistractionDetector
import com.kova.app.domain.detector.SensorFusionEngine
import com.kova.app.domain.model.PostAlarmState
import com.kova.app.domain.model.UserProfile
import com.kova.app.service.KovaMonitorService
import com.kova.app.ui.screens.AccelerometerTestScreen
import com.kova.app.ui.screens.AlarmScreen
import com.kova.app.ui.screens.AlarmSettingsScreen
import com.kova.app.ui.screens.AwakeConfirmScreen
import com.kova.app.ui.screens.MorningCheckIn
import com.kova.app.ui.screens.MorningCheckInScreen
import com.kova.app.ui.theme.KovaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

fun calculateDaysLeft(examDate: String): Int {
    return try {
        val parts = examDate.split("-")
        val examYear = parts[0].toInt()
        val examMonth = parts[1].toInt()
        val examDay = parts[2].toInt()
        val today = java.util.Calendar.getInstance()
        val exam = java.util.Calendar.getInstance().apply {
            set(examYear, examMonth - 1, examDay, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMillis = exam.timeInMillis - today.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        diffDays.coerceAtLeast(0)
    } catch (e: Exception) {
        180
    }
}

@Composable
fun AppNavigator(
    detector: DistractionDetector,
    alarmScheduler: AlarmScheduler,
    fusionEngine: SensorFusionEngine,
    initialScreen: String,
    onStartService: (String, String) -> Unit,
    onOpenPermissions: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var snoozeCount by remember { mutableIntStateOf(0) }
    var maxSnooze by remember { mutableIntStateOf(1) }
    var alarmHour by remember { mutableIntStateOf(7) }
    var alarmMinute by remember { mutableIntStateOf(0) }
    var morningCheckIn by remember { mutableStateOf<MorningCheckIn?>(null) }
    val scope = rememberCoroutineScope()
    val daysLeft = calculateDaysLeft("2026-10-01")

    LaunchedEffect(currentScreen) {
        if (currentScreen == "home") {
            scope.launch {
                while (true) {
                    if (!detector.isPaused && detector.isDistracted(userProfile)) {
                        currentScreen = "alert"
                    }
                    delay(5000)
                }
            }
        }
        if (currentScreen == "awake_confirm") {
            fusionEngine.startCollecting()
            scope.launch {
                delay(60_000L)
                val analysis = fusionEngine.analyze()
                fusionEngine.stopCollecting()
                currentScreen = when (analysis.state) {
                    PostAlarmState.UP_WITH_PHONE,
                    PostAlarmState.UP_WITHOUT_PHONE -> "morning_checkin"
                    PostAlarmState.IN_BED_WITH_PHONE,
                    PostAlarmState.IN_BED_WITHOUT_PHONE -> "alarm"
                    PostAlarmState.UNKNOWN -> "ask_awake"
                }
            }
        }
    }

    when (currentScreen) {
        "welcome" -> WelcomeScreen(
            onStart = { currentScreen = "onboarding" }
        )
        "onboarding" -> OnboardingScreen(
            onFinish = { name, goal ->
                userProfile = UserProfile(name = name, goal = goal)
                if (!detector.hasPermission()) {
                    currentScreen = "permission"
                } else {
                    onStartService(name, goal)
                    currentScreen = "home"
                }
            }
        )
        "permission" -> PermissionScreen(
            onGrantPermission = onOpenPermissions,
            onCheckPermission = {
                if (detector.hasPermission()) {
                    onStartService(userProfile.name, userProfile.goal)
                    currentScreen = "home"
                }
            }
        )
        "home" -> HomeScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            morningCheckIn = morningCheckIn,
            onOpenAlarmSettings = { currentScreen = "alarm_settings" },
            onOpenTest = { currentScreen = "accelerometer_test" }
        )
        "alarm_settings" -> AlarmSettingsScreen(
            userName = userProfile.name,
            currentHour = alarmHour,
            currentMinute = alarmMinute,
            currentMaxSnooze = maxSnooze,
            onSaveAlarm = { hour, minute, snooze ->
                alarmHour = hour
                alarmMinute = minute
                maxSnooze = snooze
                alarmScheduler.scheduleAlarm(
                    hour = hour,
                    minute = minute,
                    userName = userProfile.name,
                    userGoal = userProfile.goal,
                    daysLeft = daysLeft
                )
                currentScreen = "home"
            },
            onBack = { currentScreen = "home" }
        )
        "alarm" -> AlarmScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            daysLeft = daysLeft,
            snoozeCount = snoozeCount,
            maxSnooze = maxSnooze,
            onAwake = {
                alarmScheduler.stopAlarmSound()
                currentScreen = "awake_confirm"
            },
            onSnooze = {
                snoozeCount++
                alarmScheduler.stopAlarmSound()
                alarmScheduler.scheduleAlarm(
                    hour = alarmHour,
                    minute = alarmMinute + 5,
                    userName = userProfile.name,
                    userGoal = userProfile.goal,
                    daysLeft = daysLeft
                )
                currentScreen = "home"
            }
        )
        "awake_confirm" -> AwakeConfirmScreen(
            userName = userProfile.name,
            onStateDetected = { state ->
                fusionEngine.stopCollecting()
                currentScreen = when (state) {
                    PostAlarmState.UP_WITH_PHONE,
                    PostAlarmState.UP_WITHOUT_PHONE -> "morning_checkin"
                    else -> "alarm"
                }
            },
            onAskUser = { currentScreen = "ask_awake" }
        )
        "ask_awake" -> AskAwakeScreen(
            userName = userProfile.name,
            onConfirmAwake = { currentScreen = "morning_checkin" },
            onSnooze = {
                snoozeCount++
                currentScreen = "alarm"
            }
        )
        "morning_checkin" -> MorningCheckInScreen(
            userName = userProfile.name,
            onComplete = { checkIn ->
                morningCheckIn = checkIn
                onStartService(userProfile.name, userProfile.goal)
                currentScreen = "home"
            }
        )
        "accelerometer_test" -> AccelerometerTestScreen(
            detector = detector,
            fusionEngine = fusionEngine,
            onBack = { currentScreen = "home" }
        )
        "alert" -> AlertScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            onDismiss = { currentScreen = "home" }
        )
    }
}

@Composable
fun AskAwakeScreen(
    userName: String,
    onConfirmAwake: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "ðŸ¤”", fontSize = 52.sp, textAlign = TextAlign.Center)
            Text(
                text = "$userName, Â¿estÃ¡s levantado?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kova no pudo determinarlo\npor los sensores.",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirmAwake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
            ) {
                Text(
                    text = "SÃ­, estoy en pie",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(
                    text = "Dame 5 minutos mÃ¡s",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "KOVA",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                letterSpacing = 8.sp
            )
            Text(
                text = "Your personal focus coach.\nWhen you get distracted, I'll let you know.",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
            ) {
                Text(
                    text = "Get started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Let's get to know you",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kova needs to know your goal\nto keep you on track.",
                fontSize = 15.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name", color = Color(0xFF9E9E9E)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFFFFF),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Your main goal", color = Color(0xFF9E9E9E)) },
                placeholder = {
                    Text("e.g. Pass the police exam", color = Color(0xFF555555))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFFFFF),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && goal.isNotBlank()) {
                        onFinish(name, goal)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                ),
                enabled = name.isNotBlank() && goal.isNotBlank()
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}

@Composable
fun PermissionScreen(
    onGrantPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "One permission needed",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kova needs to see which apps you use so it can alert you when you get distracted.\n\nYour data never leaves your device.",
                fontSize = 15.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onGrantPermission()
                    scope.launch {
                        while (true) {
                            delay(1000)
                            onCheckPermission()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
            ) {
                Text(
                    text = "Grant permission",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    userName: String,
    userGoal: String,
    morningCheckIn: MorningCheckIn?,
    onOpenAlarmSettings: () -> Unit = {},
    onOpenTest: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Ready, $userName.",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Goal: $userGoal",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            morningCheckIn?.let { checkIn ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${checkIn.energyLevel.emoji} ${checkIn.energyLevel.label} Â· ${checkIn.sleepQuality.emoji} ${checkIn.sleepQuality.label}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Estudio previsto a las ${
                        String.format("%02d", checkIn.studyStartHour)
                    }:00",
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kova is watching.\nI'll alert you when you get distracted.",
                fontSize = 14.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenAlarmSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(
                    text = "â° Configurar alarma",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            OutlinedButton(
                onClick = onOpenTest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(
                    text = "ðŸ”¬ Test de sensores",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
fun AlertScreen(
    userName: String,
    userGoal: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "âš ï¸", fontSize = 52.sp, textAlign = TextAlign.Center)
            Text(
                text = "$userName, you're getting distracted.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            Text(
                text = "Remember your goal:\n$userGoal",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
            ) {
                Text(
                    text = "Back to work",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}



