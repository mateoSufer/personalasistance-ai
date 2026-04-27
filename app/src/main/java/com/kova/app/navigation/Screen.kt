package com.kova.app.navigation

sealed class Screen(val route: String) {
    object Welcome          : Screen("welcome")
    object Onboarding       : Screen("onboarding")
    object Permission       : Screen("permission")
    object Home             : Screen("home")
    object AlarmSettings    : Screen("alarm_settings")
    object Alarm            : Screen("alarm")
    object AwakeConfirm     : Screen("awake_confirm")
    object AskAwake         : Screen("ask_awake")
    object MorningCheckIn   : Screen("morning_checkin")
    object AccelerometerTest: Screen("accelerometer_test")
    object Alert            : Screen("alert")

    // Nutrición — Phase 3
    object NutritionHome    : Screen("nutrition_home")
    object LogMeal          : Screen("log_meal")
    object NutritionDetail  : Screen("nutrition_detail")
}
