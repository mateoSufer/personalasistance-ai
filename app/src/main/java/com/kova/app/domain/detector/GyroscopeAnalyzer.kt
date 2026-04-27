package com.kova.app.domain.detector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.kova.app.domain.model.GyroscopeResult
import com.kova.app.domain.model.GyroscopeSample
import com.kova.app.domain.model.SensorVote
import kotlin.math.abs
import kotlin.math.sqrt

class GyroscopeAnalyzer(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val samples = mutableListOf<GyroscopeSample>()
    private var listener: SensorEventListener? = null
    private var startTimestamp = 0L

    companion object {
        const val STABILITY_THRESHOLD = 0.05f
        const val WRIST_MOVEMENT_MIN = 0.3f
        const val WRIST_MOVEMENT_MAX = 2.5f
        const val WALKING_ROTATION_MIN = 0.05f
        const val WALKING_ROTATION_MAX = 0.5f
        const val ACTIVE_ROTATION_THRESHOLD = 2.5f
        const val POSITION_CHANGE_THRESHOLD = 1.5f
        const val POSITION_CHANGE_MIN_INTERVAL_MS = 2000L
        const val PHONE_ALONE_STABLE_RATIO = 0.88f
        const val MAX_VALID_ROTATION = 100f
    }

    fun startCollecting() {
        samples.clear()
        startTimestamp = System.currentTimeMillis()

        if (sensor == null) return

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sample = GyroscopeSample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = System.currentTimeMillis()
                )
                if (sample.rotationRate.isFinite()
                    && sample.rotationRate < MAX_VALID_ROTATION
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

    fun getLiveRotationRate(): Float {
        if (samples.isEmpty()) return 0f
        return samples.takeLast(10)
            .map { it.rotationRate }
            .filter { it.isFinite() }
            .average()
            .toFloat()
            .let { if (it.isFinite()) it else 0f }
    }

    fun getLiveStability(): Float {
        if (samples.isEmpty()) return 1f
        val stableCount = samples.takeLast(20).count { it.isStable }
        return stableCount / minOf(samples.size, 20).toFloat()
    }

    fun analyze(): GyroscopeResult {
        val endTimestamp = System.currentTimeMillis()
        val duration = endTimestamp - startTimestamp

        if (sensor == null || samples.size < 10) return buildEmptyResult(duration)

        val rotationRates = samples
            .map { it.rotationRate }
            .filter { it.isFinite() && it < MAX_VALID_ROTATION }

        if (rotationRates.isEmpty()) return buildEmptyResult(duration)

        val totalRotation = rotationRates.sum()
        val averageRotationRate = rotationRates.average().toFloat()
        val maxRotationRate = rotationRates.maxOrNull() ?: 0f
        val variance = calculateVariance(rotationRates, averageRotationRate)

        val stableSamples = samples.count { it.isStable }
        val stablePercentage = stableSamples / samples.size.toFloat()
        val longestStablePeriodMs = detectLongestStablePeriod()

        val wristMovementDetected = detectWristMovement()
        val walkingRotationDetected = detectWalkingRotation()
        val positionChanges = detectPositionChanges()

        val (vote, confidence) = determineVote(
            averageRotationRate = averageRotationRate,
            stablePercentage = stablePercentage,
            longestStablePeriodMs = longestStablePeriodMs,
            wristMovementDetected = wristMovementDetected,
            walkingRotationDetected = walkingRotationDetected,
            positionChanges = positionChanges,
            duration = duration
        )

        return GyroscopeResult(
            totalRotation = totalRotation,
            averageRotationRate = averageRotationRate,
            variance = variance,
            maxRotationRate = maxRotationRate,
            stablePercentage = stablePercentage,
            longestStablePeriodMs = longestStablePeriodMs,
            wristMovementDetected = wristMovementDetected,
            walkingRotationDetected = walkingRotationDetected,
            positionChanges = positionChanges,
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

    private fun detectLongestStablePeriod(): Long {
        var longestMs = 0L
        var stableStart: Long? = null

        for (sample in samples) {
            if (!sample.rotationRate.isFinite()) continue
            if (sample.isStable) {
                if (stableStart == null) stableStart = sample.timestamp
            } else {
                stableStart?.let { start ->
                    val dur = sample.timestamp - start
                    if (dur > longestMs) longestMs = dur
                }
                stableStart = null
            }
        }

        stableStart?.let { start ->
            samples.lastOrNull()?.let { last ->
                val dur = last.timestamp - start
                if (dur > longestMs) longestMs = dur
            }
        }

        return longestMs
    }

    private fun detectWristMovement(): Boolean {
        val wristSamples = samples.count { sample ->
            if (!sample.rotationRate.isFinite()) return@count false
            val zRotation = abs(sample.z)
            zRotation in WRIST_MOVEMENT_MIN..WRIST_MOVEMENT_MAX
        }
        return wristSamples > samples.size * 0.15f
    }

    private fun detectWalkingRotation(): Boolean {
        val walkingSamples = samples.count { sample ->
            if (!sample.rotationRate.isFinite()) return@count false
            val yRotation = abs(sample.y)
            yRotation in WALKING_ROTATION_MIN..WALKING_ROTATION_MAX
                    && sample.rotationRate < ACTIVE_ROTATION_THRESHOLD
        }
        return walkingSamples > samples.size * 0.20f
    }

    private fun detectPositionChanges(): Int {
        var changes = 0
        var lastChangeTimestamp = 0L

        for (sample in samples) {
            if (!sample.rotationRate.isFinite()) continue
            if (sample.rotationRate > POSITION_CHANGE_THRESHOLD) {
                val timeSinceLast = sample.timestamp - lastChangeTimestamp
                if (timeSinceLast > POSITION_CHANGE_MIN_INTERVAL_MS) {
                    changes++
                    lastChangeTimestamp = sample.timestamp
                }
            }
        }
        return changes
    }

    private fun determineVote(
        averageRotationRate: Float,
        stablePercentage: Float,
        longestStablePeriodMs: Long,
        wristMovementDetected: Boolean,
        walkingRotationDetected: Boolean,
        positionChanges: Int,
        duration: Long
    ): Pair<SensorVote, Float> {

        if (walkingRotationDetected && positionChanges >= 2) {
            val confidence = minOf(0.6f + positionChanges * 0.05f, 0.9f)
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        if (wristMovementDetected && !walkingRotationDetected) {
            return Pair(SensorVote.CLEARLY_IN_BED, 0.7f)
        }

        if (stablePercentage > PHONE_ALONE_STABLE_RATIO
            && longestStablePeriodMs > 2 * 60 * 1000L
        ) {
            val confidence = minOf(0.5f + stablePercentage * 0.4f, 0.9f)
            return Pair(SensorVote.PHONE_ALONE, confidence)
        }

        if (stablePercentage > 0.7f && !wristMovementDetected) {
            val confidence = stablePercentage * 0.6f
            return Pair(SensorVote.CLEARLY_IN_BED, confidence)
        }

        if (positionChanges >= 3) {
            val confidence = minOf(0.4f + positionChanges * 0.08f, 0.8f)
            return Pair(SensorVote.CLEARLY_UP, confidence)
        }

        return Pair(SensorVote.UNCERTAIN, 0.2f)
    }

    fun buildEmptyResult(duration: Long): GyroscopeResult {
        return GyroscopeResult(
            totalRotation = 0f,
            averageRotationRate = 0f,
            variance = 0f,
            maxRotationRate = 0f,
            stablePercentage = 1f,
            longestStablePeriodMs = duration,
            wristMovementDetected = false,
            walkingRotationDetected = false,
            positionChanges = 0,
            vote = SensorVote.UNCERTAIN,
            confidence = 0f,
            samplesCollected = 0,
            analysisDurationMs = duration
        )
    }
}

