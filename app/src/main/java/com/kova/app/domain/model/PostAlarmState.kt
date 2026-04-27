package com.kova.app.domain.model

enum class PostAlarmState {
    UP_WITH_PHONE,
    UP_WITHOUT_PHONE,
    IN_BED_WITH_PHONE,
    IN_BED_WITHOUT_PHONE,
    UNKNOWN
}

data class AccelerometerSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    companion object {
        const val GRAVITY = 9.80665f
    }

    val magnitude: Float
        get() = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

    val netMovement: Float
        get() {
            val raw = Math.abs(magnitude - GRAVITY)
            return if (raw.isFinite()) raw.coerceAtLeast(0f) else 0f
        }
}

data class GyroscopeSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    val rotationRate: Float
        get() {
            val rate = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            return if (rate.isFinite()) rate else 0f
        }

    val isStable: Boolean
        get() = rotationRate < STABILITY_THRESHOLD

    companion object {
        const val STABILITY_THRESHOLD = 0.05f
    }
}

data class LightSample(
    val lux: Float,
    val timestamp: Long
) {
    val category: LightCategory
        get() = when {
            lux < 5f -> LightCategory.DARK
            lux < 50f -> LightCategory.DIM
            lux < 200f -> LightCategory.INDOOR
            lux < 1000f -> LightCategory.BRIGHT
            else -> LightCategory.SUNLIGHT
        }
}

enum class LightCategory {
    DARK, DIM, INDOOR, BRIGHT, SUNLIGHT
}

data class MicrophoneSample(
    val amplitudeDb: Float,
    val timestamp: Long
) {
    val category: SoundCategory
        get() = when {
            amplitudeDb < 20f -> SoundCategory.SILENT
            amplitudeDb < 40f -> SoundCategory.QUIET
            amplitudeDb < 60f -> SoundCategory.NORMAL
            amplitudeDb < 80f -> SoundCategory.LOUD
            else -> SoundCategory.VERY_LOUD
        }
}

enum class SoundCategory {
    SILENT, QUIET, NORMAL, LOUD, VERY_LOUD
}
