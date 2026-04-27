package com.kova.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.kova.app.alarm.AlarmScheduler
import com.kova.app.domain.detector.DistractionDetector
import com.kova.app.domain.detector.SensorFusionEngine
import com.kova.app.domain.model.PostAlarmState
import com.kova.app.domain.model.UserProfile
import com.kova.app.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        ((diffMillis / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
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
        if (currentScreen == Screen.Home.route) {
            scope.launch {
                while (true) {
                    if (!detector.isPaused && detector.isDistracted(userProfile)) {
                        currentScreen = Screen.Alert.route
                    }
                    delay(5000)
                }
            }
        }
        if (currentScreen == Screen.AwakeConfirm.route) {
            fusionEngine.startCollecting()
            scope.launch {
                delay(60_000L)
                val analysis = fusionEngine.analyze()
                fusionEngine.stopCollecting()
                currentScreen = when (analysis.state) {
                    PostAlarmState.UP_WITH_PHONE,
                    PostAlarmState.UP_WITHOUT_PHONE -> Screen.MorningCheckIn.route
                    PostAlarmState.IN_BED_WITH_PHONE,
                    PostAlarmState.IN_BED_WITHOUT_PHONE -> Screen.Alarm.route
                    PostAlarmState.UNKNOWN -> Screen.AskAwake.route
                }
            }
        }
    }

    when (currentScreen) {
        Screen.Welcome.route -> WelcomeScreen(
            onStart = { currentScreen = Screen.Onboarding.route }
        )
        Screen.Onboarding.route -> OnboardingScreen(
            onFinish = { name, goal ->
                userProfile = UserProfile(name = name, goal = goal)
                if (!detector.hasPermission()) {
                    currentScreen = Screen.Permission.route
                } else {
                    onStartService(name, goal)
                    currentScreen = Screen.Home.route
                }
            }
        )
        Screen.Permission.route -> PermissionScreen(
            onGrantPermission = onOpenPermissions,
            onCheckPermission = {
                if (detector.hasPermission()) {
                    onStartService(userProfile.name, userProfile.goal)
                    currentScreen = Screen.Home.route
                }
            }
        )
        Screen.Home.route -> HomeScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            morningCheckIn = morningCheckIn,
            onOpenAlarmSettings = { currentScreen = Screen.AlarmSettings.route },
            onOpenTest = { currentScreen = Screen.AccelerometerTest.route },
            onOpenNutrition = { currentScreen = Screen.NutritionHome.route }
        )
        Screen.AlarmSettings.route -> AlarmSettingsScreen(
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
                currentScreen = Screen.Home.route
            },
            onBack = { currentScreen = Screen.Home.route }
        )
        Screen.Alarm.route -> AlarmScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            daysLeft = daysLeft,
            snoozeCount = snoozeCount,
            maxSnooze = maxSnooze,
            onAwake = {
                alarmScheduler.stopAlarmSound()
                currentScreen = Screen.AwakeConfirm.route
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
                currentScreen = Screen.Home.route
            }
        )
        Screen.AwakeConfirm.route -> AwakeConfirmScreen(
            userName = userProfile.name,
            onStateDetected = { state ->
                fusionEngine.stopCollecting()
                currentScreen = when (state) {
                    PostAlarmState.UP_WITH_PHONE,
                    PostAlarmState.UP_WITHOUT_PHONE -> Screen.MorningCheckIn.route
                    else -> Screen.Alarm.route
                }
            },
            onAskUser = { currentScreen = Screen.AskAwake.route }
        )
        Screen.AskAwake.route -> AskAwakeScreen(
            userName = userProfile.name,
            onConfirmAwake = { currentScreen = Screen.MorningCheckIn.route },
            onSnooze = {
                snoozeCount++
                currentScreen = Screen.Alarm.route
            }
        )
        Screen.MorningCheckIn.route -> MorningCheckInScreen(
            userName = userProfile.name,
            onComplete = { checkIn ->
                morningCheckIn = checkIn
                onStartService(userProfile.name, userProfile.goal)
                currentScreen = Screen.Home.route
            }
        )
        Screen.AccelerometerTest.route -> AccelerometerTestScreen(
            detector = detector,
            fusionEngine = fusionEngine,
            onBack = { currentScreen = Screen.Home.route }
        )
        Screen.Alert.route -> AlertScreen(
            userName = userProfile.name,
            userGoal = userProfile.goal,
            onDismiss = { currentScreen = Screen.Home.route }
        )
        Screen.NutritionHome.route -> PlaceholderNutritionScreen(
            onBack = { currentScreen = Screen.Home.route }
        )
    }
}

@Composable
fun PlaceholderNutritionScreen(onBack: () -> Unit) {
    androidx.compose.material3.Button(onClick = onBack) {
        androidx.compose.material3.Text("Nutrición — próximamente")
    }
}
