package com.kova.app.domain.detector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.kova.app.domain.model.LightCategory
import com.kova.app.domain.model.LightResult
import com.kova.app.domain.model.LightSample
import com.kova.app.domain.model.SensorVote
import kotlin.math.abs

class LightAnalyzer(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val samples = mutableListOf<LightSample>()
    private var listener: SensorEventListener? = null
    private var startTimestamp = 0L

    companion object {
        // ── Umbrales de cambio de luz ─────────────────

        // Cambio brusco de luz — encendió/apagó la luz
        const val SIGNIFICANT_CHANGE_LUX = 50f

        // Cambio de habitación — luz sube mucho de golpe
        const val ROOM_CHANGE_LUX = 150f

        // Intervalo mínimo entre cambios significativos
        const val MIN_CHANGE_INTERVAL_MS = 3000L

        // Luz de pantalla de TV parpadeando
        const val TV_FLICKER_MIN_LUX = 5f
        const val TV_FLICKER_MAX_LUX = 80f
        const val TV_FLICKER_VARIANCE_MIN = 10f

        // Si la luz promedio es esta de baja → habitación oscura
        const val DARK_ROOM_MAX_LUX = 10f

        // Si la luz sube por encima de esto → encendió la luz
        const val LIGHT_ON_THRESHOLD_LUX = 80f
    }

    // ── Recogida de datos ────────────────────────────

    fun startCollecting() {
        samples.clear()
        startTimestamp = System.currentTimeMillis()

        if (sensor == null) return

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                samples.add(
                    LightSample(
                        lux = event.values[0],
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stopCollecting() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }

    fun clearSamples() {
        samples.clear()
        startTimestamp = 0L
    }

    // ── Métricas en tiempo real ──────────────────────

    fun getSampleCount(): Int = samples.size

    fun getLiveLux(): Float {
        if (samples.isEmpty()) return 0f
        return samples.last().lux
    }

    fun getLiveCategory(): LightCategory {
        if (samples.isEmpty()) return LightCategory.DARK
        return samples.last().category
    }

    // ── Análisis completo ────────────────────────────

    fun analyze(): LightResult {
        val endTimestamp = System.currentTimeMillis()
        val duration = endTimestamp - startTimestamp

        if (sensor == null || samples.size < 5) {
            return buildEmptyResult(duration)
        }

        val luxValues = samples.map { it.lux }

        // Métricas básicas
        val averageLux = luxValues.average().toFloat()
        val minLux = luxValues.minOrNull() ?: 0f
        val maxLux = luxValues.maxOrNull() ?: 0f
        val mean = averageLux
        val luxVariance = luxValues
            .map { (it - mean) * (it - mean) }
            .average()
            .toFloat()

        // Categoría dominante
        val dominantCategory = samples
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key ?: LightCategory.DARK

        // Cambios significativos
        val significantChanges = detectSignificantChanges()

        // Eventos específicos
        val lightTurnedOn = detectLightTurnedOn()
        val lightTurnedOff = detectLightTurnedOff()
        val movedToNewRoom = detectRoomChange()

        // Voto y confianza
        val (vote, confidence) = determineVote(
            averageLux = averageLux,
            dominantCategory = dominantCategory,
            significantChanges = significantChanges,
            lightTurnedOn = lightTurnedOn,
            movedToNewRoom = movedToNewRoom,
            luxVariance = luxVariance
        )

        return LightResult(
            averageLux = averageLux,
            minLux = minLux,
            maxLux = maxLux,
            luxVariance = luxVariance,
            significantChanges = significantChanges,
            lightTurnedOn = lightTurnedOn,
            lightTurnedOff = lightTurnedOff,
            movedToNewRoom = movedToNewRoom,
            dominantCategory = dominantCategory,
            vote = vote,
            confidence = confidence,
            samplesCollected = samples.size,
            analysisDurationMs = duration
        )
    }

    // ── Algoritmos internos ──────────────────────────

    private fun detectSignificantChanges(): Int {
        // Detecta cambios bruscos de iluminación
        // que indican que el usuario encendió/apagó luz
        // o se movió a otra habitación
        var changes = 0
        var lastChangeTimestamp = 0L

        for (i in 1 until samples.size) {
            val diff = abs(samples[i].lux - samples[i - 1].lux)
            val timeSinceLast = samples[i].timestamp - lastChangeTimestamp

            if (diff > SIGNIFICANT_CHANGE_LUX
                && timeSinceLast > MIN_CHANGE_INTERVAL_MS
            ) {
                changes++
                lastChangeTimestamp = samples[i].timestamp
            }
        }

        return changes
    }

    private fun detectLightTurnedOn(): Boolean {
        // Detecta si la luz de la habitación se encendió
        // La luz sube bruscamente desde un nivel bajo
        // y se mantiene alta durante al menos 10 segundos
        if (samples.size < 20) return false

        val firstQuarter = samples.take(samples.size / 4)
        val lastQuarter = samples.takeLast(samples.size / 4)

        val avgFirst = firstQuarter.map { it.lux }.average().toFloat()
        val avgLast = lastQuarter.map { it.lux }.average().toFloat()

        return avgFirst < DARK_ROOM_MAX_LUX
                && avgLast > LIGHT_ON_THRESHOLD_LUX
                && (avgLast - avgFirst) > SIGNIFICANT_CHANGE_LUX
    }

    private fun detectLightTurnedOff(): Boolean {
        // Detecta si la luz se apagó
        if (samples.size < 20) return false

        val firstQuarter = samples.take(samples.size / 4)
        val lastQuarter = samples.takeLast(samples.size / 4)

        val avgFirst = firstQuarter.map { it.lux }.average().toFloat()
        val avgLast = lastQuarter.map { it.lux }.average().toFloat()

        return avgFirst > LIGHT_ON_THRESHOLD_LUX
                && avgLast < DARK_ROOM_MAX_LUX
                && (avgFirst - avgLast) > SIGNIFICANT_CHANGE_LUX
    }

    private fun detectRoomChange(): Boolean {
        // Detecta si el usuario se movió a otra habitación
        // La luz cambia bruscamente y se mantiene en nuevo nivel
        if (samples.size < 10) return false

        for (i in 1 until samples.size) {
            val diff = abs(samples[i].lux - samples[i - 1].lux)
            if (diff > ROOM_CHANGE_LUX) {
                // Verifica que el nuevo nivel se mantiene
                val remaining = samples.drop(i)
                if (remaining.size >= 5) {
                    val avgRemaining = remaining
                        .take(10)
                        .map { it.lux }
                        .average()
                        .toFloat()
                    val newLevel = samples[i].lux
                    if (abs(avgRemaining - newLevel) < SIGNIFICANT_CHANGE_LUX) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun determineVote(
        averageLux: Float,
        dominantCategory: LightCategory,
        significantChanges: Int,
        lightTurnedOn: Boolean,
        movedToNewRoom: Boolean,
        luxVariance: Float
    ): Pair<SensorVote, Float> {

        // ── Caso 1: Encendió la luz ───────────────────
        // Señal muy fuerte de que se levantó
        if (lightTurnedOn) {
            val confidence = 0.75f
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        // ── Caso 2: Se movió de habitación ───────────
        // Cambio brusco y sostenido de luz
        if (movedToNewRoom) {
            val confidence = 0.7f
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        // ── Caso 3: Oscuridad total constante ────────
        // Habitación completamente oscura — dormido
        if (dominantCategory == LightCategory.DARK
            && significantChanges == 0
        ) {
            val confidence = 0.65f
            return Pair(SensorVote.CLEARLY_IN_BED, confidence)
        }

        // ── Caso 4: Luz estable baja ──────────────────
        // Habitación con poca luz, sin cambios
        // Podría ser cama con luz tenue o móvil solo
        if (dominantCategory == LightCategory.DIM
            && significantChanges == 0
            && luxVariance < TV_FLICKER_VARIANCE_MIN
        ) {
            return Pair(SensorVote.UNCERTAIN, 0.35f)
        }

        // ── Caso 5: Cambios múltiples de luz ─────────
        // El usuario se ha movido por distintas zonas
        if (significantChanges >= 2) {
            val confidence = minOf(0.5f + significantChanges * 0.1f, 0.8f)
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        // ── Caso 6: Luz brillante constante ──────────
        // Habitación con luz encendida desde el principio
        if (dominantCategory == LightCategory.BRIGHT
            || dominantCategory == LightCategory.INDOOR
        ) {
            return Pair(SensorVote.UNCERTAIN, 0.3f)
        }

        // ── Caso por defecto ──────────────────────────
        return Pair(SensorVote.UNCERTAIN, 0.15f)
    }

    private fun buildEmptyResult(duration: Long): LightResult {
        return LightResult(
            averageLux = 0f,
            minLux = 0f,
            maxLux = 0f,
            luxVariance = 0f,
            significantChanges = 0,
            lightTurnedOn = false,
            lightTurnedOff = false,
            movedToNewRoom = false,
            dominantCategory = LightCategory.DARK,
            vote = SensorVote.UNCERTAIN,
            confidence = 0f,
            samplesCollected = 0,
            analysisDurationMs = duration
        )
    }
}