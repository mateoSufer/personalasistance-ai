package com.kova.app.domain.detector

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import com.kova.app.domain.model.UserProfile

class DistractionDetector(private val context: Context) {

    var isPaused: Boolean = false


    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getCurrentApp(): String {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 10
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
    }

    fun isDistracted(profile: UserProfile): Boolean {
        val currentApp = getCurrentApp()
        return profile.distractionApps.any { currentApp.contains(it) }
    }

    fun getDistractingApp(profile: UserProfile): String {
        val currentApp = getCurrentApp()
        return profile.distractionApps.firstOrNull { currentApp.contains(it) } ?: ""
    }
}
