package com.kova.app.domain.detector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.kova.app.domain.model.AccelerometerResult
import com.kova.app.domain.model.AccelerometerSample
import com.kova.app.domain.model.SensorVote
import kotlin.math.abs
import kotlin.math.sqrt

class AccelerometerAnalyzer(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val samples = mutableListOf<AccelerometerSample>()
    private var listener: SensorEventListener? = null
    private var startTimestamp = 0L

    companion object {
        const val NOISE_FLOOR = 0.02f
        const val MICRO_MOVEMENT_MIN = 0.02f
        const val MICRO_MOVEMENT_MAX = 0.5f
        const val ACTIVE_MOVEMENT_THRESHOLD = 0.5f
        const val WALKING_THRESHOLD = 0.6f
        const val PHONE_ALONE_FLAT_MS = 30 * 1000L
        const val STEP_MIN_INTERVAL_MS = 300L
        const val STEP_MAX_INTERVAL_MS = 1000L
        const val ANALYSIS_DURATION_MS = 60 * 1000L
        const val MAX_VALID_MOVEMENT = 50f
    }

    fun startCollecting() {
        samples.clear()
        startTimestamp = System.currentTimeMillis()

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sample = AccelerometerSample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = System.currentTimeMillis()
                )
                if (sample.netMovement.isFinite()
                    && sample.netMovement < MAX_VALID_MOVEMENT
                ) {
                    samples.add(sample)
                }
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

    fun getSampleCount(): Int = samples.size

    fun getLiveAverageMovement(): Float {
        if (samples.isEmpty()) return 0f
        return samples.takeLast(10)
            .map { it.netMovement }
            .filter { it.isFinite() }
            .average()
            .toFloat()
            .let { if (it.isFinite()) it else 0f }
    }

    fun getLiveVariance(): Float {
        if (samples.size < 2) return 0f
        val recent = samples.takeLast(20)
            .map { it.netMovement }
            .filter { it.isFinite() }
        if (recent.isEmpty()) return 0f
        val avg = recent.average().toFloat()
        return recent.map { (it - avg) * (it - avg) }
            .average()
            .toFloat()
            .let { if (it.isFinite()) it else 0f }
    }

    fun analyze(): AccelerometerResult {
        val endTimestamp = System.currentTimeMillis()
        val duration = endTimestamp - startTimestamp

        if (samples.size < 10) return buildEmptyResult(duration)

        val netMovements = samples
            .map { it.netMovement }
            .filter { it.isFinite() && it < MAX_VALID_MOVEMENT }

        if (netMovements.isEmpty()) return buildEmptyResult(duration)

        val totalNetMovement = netMovements.sum()
        val averageNetMovement = netMovements.average().toFloat()
        val variance = calculateVariance(netMovements, averageNetMovement)
        val standardDeviation = if (variance.isFinite()) sqrt(variance) else 0f

        val flatPeriods = detectFlatPeriods()
        val longestFlatPeriodMs = flatPeriods.maxOrNull() ?: 0L
        val flatPeriodCount = flatPeriods.size
        val totalFlatMs = flatPeriods.sum()
        val flatTimeRatio = if (duration > 0)
            (totalFlatMs / duration.toFloat()).coerceIn(0f, 1f) else 0f

        val steps = detectSteps()
        val stepCount = steps.size
        val walkingPeriodMs = calculateWalkingPeriod(steps)
        val walkingTimeRatio = if (duration > 0)
            (walkingPeriodMs / duration.toFloat()).coerceIn(0f, 1f) else 0f

        val (vote, confidence) = determineVote(
            averageNetMovement = averageNetMovement,
            variance = variance,
            longestFlatPeriodMs = longestFlatPeriodMs,
            flatTimeRatio = flatTimeRatio,
            walkingTimeRatio = walkingTimeRatio,
            stepCount = stepCount
        )

        return AccelerometerResult(
            totalNetMovement = totalNetMovement,
            averageNetMovement = averageNetMovement,
            variance = variance,
            standardDeviation = standardDeviation,
            longestFlatPeriodMs = longestFlatPeriodMs,
            flatPeriodCount = flatPeriodCount,
            flatTimeRatio = flatTimeRatio,
            stepCount = stepCount,
            walkingPeriodMs = walkingPeriodMs,
            walkingTimeRatio = walkingTimeRatio,
            vote = vote,
            confidence = confidence,
            samplesCollected = samples.size,
            analysisDurationMs = duration
        )
    }

    private fun calculateVariance(values: List<Float>, mean: Float): Float {
        if (values.size < 2) return 0f
        val variance = values.map { (it - mean) * (it - mean) }
            .average()
            .toFloat()
        return if (variance.isFinite()) variance else 0f
    }

    private fun detectFlatPeriods(): List<Long> {
        val flatPeriods = mutableListOf<Long>()
        var flatStart: Long? = null

        for (sample in samples) {
            if (!sample.netMovement.isFinite()) continue
            if (sample.netMovement < NOISE_FLOOR) {
                if (flatStart == null) flatStart = sample.timestamp
            } else {
                flatStart?.let { start ->
                    val dur = sample.timestamp - start
                    if (dur > 1000) flatPeriods.add(dur)
                }
                flatStart = null
            }
        }

        flatStart?.let { start ->
            samples.lastOrNull()?.let { last ->
                val dur = last.timestamp - start
                if (dur > 1000) flatPeriods.add(dur)
            }
        }

        return flatPeriods
    }

    private fun detectSteps(): List<Long> {
        val stepTimestamps = mutableListOf<Long>()
        var lastPeakTimestamp = 0L
        var inPeak = false

        for (sample in samples) {
            if (!sample.netMovement.isFinite()) continue
            if (sample.netMovement > WALKING_THRESHOLD) {
                if (!inPeak) {
                    val interval = sample.timestamp - lastPeakTimestamp
                    if (lastPeakTimestamp == 0L ||
                        interval in STEP_MIN_INTERVAL_MS..STEP_MAX_INTERVAL_MS
                    ) {
                        stepTimestamps.add(sample.timestamp)
                        lastPeakTimestamp = sample.timestamp
                    }
                    inPeak = true
                }
            } else {
                inPeak = false
            }
        }
        return stepTimestamps
    }

    private fun calculateWalkingPeriod(stepTimestamps: List<Long>): Long {
        if (stepTimestamps.size < 2) return 0L
        var walkingMs = 0L
        for (i in 1 until stepTimestamps.size) {
            val interval = stepTimestamps[i] - stepTimestamps[i - 1]
            if (interval <= STEP_MAX_INTERVAL_MS) walkingMs += interval
        }
        return walkingMs
    }

    private fun determineVote(
        averageNetMovement: Float,
        variance: Float,
        longestFlatPeriodMs: Long,
        flatTimeRatio: Float,
        walkingTimeRatio: Float,
        stepCount: Int
    ): Pair<SensorVote, Float> {

        if (stepCount >= 5 && walkingTimeRatio > 0.15f) {
            val confidence = minOf(0.6f + (stepCount / 50f) + walkingTimeRatio, 1.0f)
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        if (averageNetMovement > ACTIVE_MOVEMENT_THRESHOLD) {
            val confidence = minOf(0.5f + (averageNetMovement / 10f), 0.85f)
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        if (longestFlatPeriodMs > PHONE_ALONE_FLAT_MS || (variance > 0.05f && averageNetMovement < 0.5f)) {
            val confidence = minOf(
                0.6f + (longestFlatPeriodMs / PHONE_ALONE_FLAT_MS.toFloat() * 0.2f),
                0.9f
            )
            return Pair(SensorVote.PHONE_ALONE, confidence)
        }

        if (averageNetMovement in MICRO_MOVEMENT_MIN..MICRO_MOVEMENT_MAX
            && variance in 0.001f..0.5f
            && longestFlatPeriodMs < PHONE_ALONE_FLAT_MS
        ) {
            val confidence = minOf(0.4f + (variance / 0.5f) * 0.4f, 0.8f)
            return Pair(SensorVote.CLEARLY_IN_BED, confidence)
        }

        if (flatTimeRatio > 0.7f) {
            return Pair(SensorVote.UNCERTAIN, 0.3f)
        }

        return Pair(SensorVote.UNCERTAIN, 0.1f)
    }

    fun buildEmptyResult(duration: Long): AccelerometerResult {
        return AccelerometerResult(
            totalNetMovement = 0f,
            averageNetMovement = 0f,
            variance = 0f,
            standardDeviation = 0f,
            longestFlatPeriodMs = 0L,
            flatPeriodCount = 0,
            flatTimeRatio = 0f,
            stepCount = 0,
            walkingPeriodMs = 0L,
            walkingTimeRatio = 0f,
            vote = SensorVote.UNCERTAIN,
            confidence = 0f,
            samplesCollected = samples.size,
            analysisDurationMs = duration
        )
    }
}


