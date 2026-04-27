package com.kova.app.domain.detector

import android.content.Context
import android.os.PowerManager
import android.app.usage.UsageStatsManager
import com.kova.app.domain.model.PostAlarmAnalysis
import com.kova.app.domain.model.PostAlarmState
import com.kova.app.domain.model.SensorVote
import com.kova.app.domain.model.SensorWeights
import com.kova.app.domain.model.WeightedVote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SensorFusionEngine(private val context: Context) {

    private val accelerometerAnalyzer = AccelerometerAnalyzer(context)
    private val gyroscopeAnalyzer = GyroscopeAnalyzer(context)
    private val lightAnalyzer = LightAnalyzer(context)
    private val microphoneAnalyzer = MicrophoneAnalyzer(context)

    private val weights = SensorWeights()

    companion object {
        const val RELIABILITY_THRESHOLD = 0.50f
        const val DECISION_THRESHOLD = 0.20f
    }

    fun startCollecting() {
        accelerometerAnalyzer.startCollecting()
        gyroscopeAnalyzer.startCollecting()
        lightAnalyzer.startCollecting()
    }

    fun stopCollecting() {
        accelerometerAnalyzer.stopCollecting()
        gyroscopeAnalyzer.stopCollecting()
        lightAnalyzer.stopCollecting()
    }

    fun clearAll() {
        accelerometerAnalyzer.clearSamples()
        gyroscopeAnalyzer.clearSamples()
        lightAnalyzer.clearSamples()
    }

    fun getLiveSensorData(): Map<String, Float> {
        return mapOf(
            "accel_movement" to accelerometerAnalyzer.getLiveAverageMovement(),
            "accel_variance" to accelerometerAnalyzer.getLiveVariance(),
            "gyro_rotation" to gyroscopeAnalyzer.getLiveRotationRate(),
            "gyro_stability" to gyroscopeAnalyzer.getLiveStability(),
            "light_lux" to lightAnalyzer.getLiveLux(),
            "mic_db" to 0f
        )
    }

    fun getTotalSamples(): Map<String, Int> {
        return mapOf(
            "accelerometer" to accelerometerAnalyzer.getSampleCount(),
            "gyroscope" to gyroscopeAnalyzer.getSampleCount(),
            "light" to lightAnalyzer.getSampleCount(),
            "microphone" to 0
        )
    }

    suspend fun analyze(): PostAlarmAnalysis = coroutineScope {
        val startTimestamp = System.currentTimeMillis()
        val analysisDurationMs = 60_000L

        val accelDeferred = async { accelerometerAnalyzer.analyze() }
        val gyroDeferred = async { gyroscopeAnalyzer.analyze() }
        val lightDeferred = async { lightAnalyzer.analyze() }

        val accelResult = accelDeferred.await()
        val gyroResult = gyroDeferred.await()
        val lightResult = lightDeferred.await()
        val micResult = microphoneAnalyzer.buildEmptyResult(0L)

        val endTimestamp = System.currentTimeMillis()

        // ── Lógica de fusión basada en datos reales del Oppo ──
        // Datos calibrados:
        // C (caminar+dejar): mov=0.518, var=0.507, estable=47%, luz=19.3
        // A (levantado sin móvil): mov=0.224, var=0.02, estable=84%, luz=3.2
        // B (cama sin móvil): mov=0.195, var=0.00001, estable=89%, luz=0

        val avgMovement = accelResult.averageNetMovement
        val variance = accelResult.variance
        val stablePct = gyroResult.stablePercentage
        val avgLux = lightResult.averageLux
        val lightTurnedOn = lightResult.lightTurnedOn
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        // Detecta si el usuario usó otras apps durante el análisis
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - analysisDurationMs
        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
        )
        val usedOtherApp = stats?.any { stat ->
            stat.packageName != context.packageName
                && stat.totalTimeInForeground > 2000
                && stat.lastTimeUsed > beginTime
        } ?: false
        val lightChanges = lightResult.significantChanges

        val (finalState, globalConfidence) = when {

            // ── USÓ OTRA APP = móvil en mano ────────────────
            // Si usó cualquier app que no sea Kova durante el análisis
            // claramente tenía el móvil en mano
            usedOtherApp && avgLux > 3f -> {
                Pair(PostAlarmState.UP_WITH_PHONE, 0.85f)
            }

            usedOtherApp && avgLux <= 3f -> {
                Pair(PostAlarmState.IN_BED_WITH_PHONE, 0.85f)
            }

            // ── PANTALLA ENCENDIDA = móvil en mano ────────────
            // Si la pantalla está encendida el usuario
            // claramente tiene el móvil en mano mirándolo
            isScreenOn && avgLux > 3f -> {
                Pair(PostAlarmState.UP_WITH_PHONE, 0.80f)
            }

            isScreenOn && avgLux <= 3f -> {
                Pair(PostAlarmState.IN_BED_WITH_PHONE, 0.80f)
            }


            // ── ESCENARIO C: Caminó con el móvil ─────────────
            // Alta varianza + movimiento moderado-alto
            variance > 0.3f && avgMovement > 0.4f -> {
                Pair(PostAlarmState.UP_WITH_PHONE, 0.75f)
            }

            // ── ESCENARIO C parcial: movió el móvil un poco ──
            variance > 0.1f && avgMovement > 0.35f -> {
                Pair(PostAlarmState.UP_WITH_PHONE, 0.60f)
            }

            // ── ESCENARIO B: En cama, móvil en mesilla ────────
            // Varianza casi cero + oscuridad + muy estable
            variance < 0.001f && avgLux < 1f && stablePct > 0.85f -> {
                Pair(PostAlarmState.IN_BED_WITHOUT_PHONE, 0.80f)
            }

            // ── ESCENARIO B variante: cama sin luz ────────────
            variance < 0.01f && avgLux < 2f && avgMovement < 0.25f -> {
                Pair(PostAlarmState.IN_BED_WITHOUT_PHONE, 0.70f)
            }

            // ── ESCENARIO A: Levantado, móvil en mesilla ──────
            // Varianza baja pero no nula + algo de luz
            // o encendió la luz en algún momento
            variance in 0.01f..0.15f && avgLux > 2f -> {
                Pair(PostAlarmState.UP_WITHOUT_PHONE, 0.70f)
            }

            lightTurnedOn || lightChanges >= 1 -> {
                Pair(PostAlarmState.UP_WITHOUT_PHONE, 0.65f)
            }

            variance in 0.005f..0.15f && avgMovement in 0.2f..0.4f -> {
                Pair(PostAlarmState.UP_WITHOUT_PHONE, 0.55f)
            }

            // ── ESCENARIO A sin luz: varianza moderada ─────────
            variance > 0.01f && stablePct < 0.90f && avgLux < 2f -> {
                Pair(PostAlarmState.UP_WITHOUT_PHONE, 0.50f)
            }

            // ── No determinado ─────────────────────────────────
            else -> Pair(PostAlarmState.UNKNOWN, 0.15f)
        }

        // Construye votos para mostrar en la UI
        val accelVote = WeightedVote(
            sensorName = "Acelerómetro",
            vote = if (avgMovement > 0.4f) SensorVote.CLEARLY_UP
                   else if (variance < 0.001f) SensorVote.CLEARLY_IN_BED
                   else SensorVote.UNCERTAIN,
            confidence = accelResult.confidence,
            weight = weights.accelerometer
        )
        val gyroVote = WeightedVote(
            sensorName = "Giroscopio",
            vote = gyroResult.vote,
            confidence = gyroResult.confidence,
            weight = weights.gyroscope
        )
        val lightVote = WeightedVote(
            sensorName = "Luz",
            vote = if (avgLux > 5f) SensorVote.CLEARLY_UP
                   else if (avgLux < 1f) SensorVote.CLEARLY_IN_BED
                   else SensorVote.UNCERTAIN,
            confidence = lightResult.confidence,
            weight = weights.light
        )
        val micVote = WeightedVote(
            sensorName = "Micrófono",
            vote = SensorVote.UNCERTAIN,
            confidence = 0f,
            weight = weights.microphone
        )

        PostAlarmAnalysis(
            state = finalState,
            confidence = globalConfidence,
            isReliable = globalConfidence >= RELIABILITY_THRESHOLD,
            accelerometerVote = accelVote,
            gyroscopeVote = gyroVote,
            microphoneVote = micVote,
            lightVote = lightVote,
            accelerometerResult = accelResult,
            gyroscopeResult = gyroResult,
            microphoneResult = micResult,
            lightResult = lightResult,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            analysisDurationMs = endTimestamp - startTimestamp,
            weightsUsed = weights
        )
    }
}







