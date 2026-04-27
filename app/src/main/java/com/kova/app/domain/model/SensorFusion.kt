package com.kova.app.domain.model

// ════════════════════════════════════════════════════
// 3. RESULTADO PARCIAL DE CADA SENSOR
// Cada analizador produce uno de estos resultados.
// El motor de fusión los recibe todos y decide.
// ════════════════════════════════════════════════════

/**
 * El voto que cada sensor puede emitir.
 * Ningún sensor puede determinar el estado final solo,
 * pero cada uno puede votar con un nivel de confianza.
 */
enum class SensorVote {
    CLEARLY_UP,         // Señal clara de que el usuario está levantado
    CLEARLY_IN_BED,     // Señal clara de que está en cama
    PHONE_ALONE,        // El móvil está solo, sin el usuario cerca
    UNCERTAIN           // Señal insuficiente para votar
}

/**
 * Resultado del análisis del acelerómetro.
 * Detecta movimiento lineal: caminar, levantarse, quieto.
 */
data class AccelerometerResult(
    // Métricas de movimiento
    val totalNetMovement: Float,        // Suma total de movimiento neto
    val averageNetMovement: Float,      // Promedio de movimiento por muestra
    val variance: Float,                // Irregularidad del movimiento
    val standardDeviation: Float,       // Desviación estándar

    // Detección de quietud
    val longestFlatPeriodMs: Long,      // Periodo más largo sin movimiento
    val flatPeriodCount: Int,           // Número de periodos quietos
    val flatTimeRatio: Float,           // % del tiempo quieto sobre el total

    // Detección de pasos
    val stepCount: Int,                 // Pasos estimados
    val walkingPeriodMs: Long,          // Tiempo total caminando
    val walkingTimeRatio: Float,        // % del tiempo caminando

    // Decisión del sensor
    val vote: SensorVote,
    val confidence: Float,              // 0.0 sin confianza → 1.0 certeza total
    val samplesCollected: Int,
    val analysisDurationMs: Long
) {
    // Descripción legible para logging y debug
    val summary: String
        get() = """
            [ACELERÓMETRO] Vote: $vote (${(confidence * 100).toInt()}%)
            Movimiento total: ${"%.3f".format(totalNetMovement)}
            Movimiento promedio: ${"%.4f".format(averageNetMovement)}
            Varianza: ${"%.5f".format(variance)}
            Periodo quieto más largo: ${longestFlatPeriodMs / 1000}s
            % tiempo quieto: ${"%.1f".format(flatTimeRatio * 100)}%
            Pasos detectados: $stepCount
            % tiempo caminando: ${"%.1f".format(walkingTimeRatio * 100)}%
            Muestras: $samplesCollected en ${analysisDurationMs / 1000}s
        """.trimIndent()
}

/**
 * Resultado del análisis del giroscopio.
 * Detecta rotación: postura, movimiento de muñeca, pasos.
 */
data class GyroscopeResult(
    // Métricas de rotación
    val totalRotation: Float,           // Rotación total acumulada
    val averageRotationRate: Float,     // Velocidad de rotación promedio
    val variance: Float,                // Irregularidad de la rotación
    val maxRotationRate: Float,         // Pico máximo de rotación

    // Estabilidad
    val stablePercentage: Float,        // % del tiempo estable (sin rotación)
    val longestStablePeriodMs: Long,    // Periodo más largo sin rotar

    // Patrones específicos
    val wristMovementDetected: Boolean, // Movimiento típico de uso del móvil
    val walkingRotationDetected: Boolean, // Rotación rítmica típica de caminar
    val positionChanges: Int,           // Número de cambios de postura

    // Decisión del sensor
    val vote: SensorVote,
    val confidence: Float,
    val samplesCollected: Int,
    val analysisDurationMs: Long
) {
    val summary: String
        get() = """
            [GIROSCOPIO] Vote: $vote (${(confidence * 100).toInt()}%)
            Rotación promedio: ${"%.4f".format(averageRotationRate)} rad/s
            Rotación máxima: ${"%.4f".format(maxRotationRate)} rad/s
            % tiempo estable: ${"%.1f".format(stablePercentage * 100)}%
            Periodo estable más largo: ${longestStablePeriodMs / 1000}s
            Movimiento de muñeca: $wristMovementDetected
            Rotación de caminar: $walkingRotationDetected
            Cambios de postura: $positionChanges
            Muestras: $samplesCollected en ${analysisDurationMs / 1000}s
        """.trimIndent()
}

/**
 * Resultado del análisis del sensor de luz.
 * Detecta contexto lumínico: oscuro, encendió luz, salió.
 */
data class LightResult(
    // Métricas de luz
    val averageLux: Float,              // Luz promedio durante el análisis
    val minLux: Float,                  // Luz mínima detectada
    val maxLux: Float,                  // Luz máxima detectada
    val luxVariance: Float,             // Variación de luz

    // Cambios de luz
    val significantChanges: Int,        // Cambios bruscos de iluminación
    val lightTurnedOn: Boolean,         // Encendió la luz de la habitación
    val lightTurnedOff: Boolean,        // Apagó la luz
    val movedToNewRoom: Boolean,        // Cambio de nivel consistente con moverse

    // Categoría dominante
    val dominantCategory: LightCategory, // La categoría más frecuente

    // Decisión del sensor
    val vote: SensorVote,
    val confidence: Float,
    val samplesCollected: Int,
    val analysisDurationMs: Long
) {
    val summary: String
        get() = """
            [LUZ] Vote: $vote (${(confidence * 100).toInt()}%)
            Lux promedio: ${"%.1f".format(averageLux)}
            Rango: ${"%.1f".format(minLux)} - ${"%.1f".format(maxLux)}
            Variación: ${"%.2f".format(luxVariance)}
            Categoría dominante: $dominantCategory
            Encendió luz: $lightTurnedOn
            Se movió de habitación: $movedToNewRoom
            Cambios significativos: $significantChanges
            Muestras: $samplesCollected en ${analysisDurationMs / 1000}s
        """.trimIndent()
}

/**
 * Resultado del análisis del micrófono.
 * Detecta ambiente sonoro: silencio, pasos, agua, TV.
 */
data class MicrophoneResult(
    // Métricas de sonido
    val averageDb: Float,               // Volumen promedio
    val minDb: Float,                   // Volumen mínimo
    val maxDb: Float,                   // Volumen máximo
    val variance: Float,                // Variación del volumen

    // Categoría dominante
    val dominantCategory: SoundCategory, // La categoría más frecuente

    // Patrones detectados
    val footstepsDetected: Boolean,     // Patrón rítmico de pasos
    val waterSoundDetected: Boolean,    // Sonido de agua (ducha, grifo)
    val tvOrMusicDetected: Boolean,     // Sonido continuo de TV o música
    val silentPeriodRatio: Float,       // % del tiempo en silencio

    // Decisión del sensor
    val vote: SensorVote,
    val confidence: Float,
    val samplesCollected: Int,
    val analysisDurationMs: Long
) {
    val summary: String
        get() = """
            [MICRÓFONO] Vote: $vote (${(confidence * 100).toInt()}%)
            Volumen promedio: ${"%.1f".format(averageDb)} dB
            Rango: ${"%.1f".format(minDb)} - ${"%.1f".format(maxDb)} dB
            Categoría dominante: $dominantCategory
            Pasos detectados: $footstepsDetected
            Agua detectada: $waterSoundDetected
            TV/música detectada: $tvOrMusicDetected
            % tiempo silencio: ${"%.1f".format(silentPeriodRatio * 100)}%
            Muestras: $samplesCollected en ${analysisDurationMs / 1000}s
        """.trimIndent()
}

// ════════════════════════════════════════════════════
// 4. VOTO PONDERADO DE CADA SENSOR
// Cada sensor tiene un peso diferente según su
// fiabilidad para cada tipo de detección
// ════════════════════════════════════════════════════

/**
 * El peso de cada sensor en la decisión final.
 * Estos pesos reflejan la fiabilidad de cada sensor
 * para detectar el estado post-alarma.
 *
 * El total de pesos suma 1.0 (100%)
 */
data class SensorWeights(
    val accelerometer: Float = 0.35f,   // 35% — movimiento lineal muy fiable
    val gyroscope: Float = 0.25f,       // 25% — rotación complementa bien
    val microphone: Float = 0.25f,      // 25% — ambiente sonoro muy revelador
    val light: Float = 0.15f            // 15% — contexto útil pero ambiguo
) {
    init {
        val total = accelerometer + gyroscope + microphone + light
        require(Math.abs(total - 1.0f) < 0.01f) {
            "Los pesos deben sumar 1.0, suman $total"
        }
    }
}

/**
 * El voto ponderado de un sensor individual.
 * Combina el voto con su peso y confianza.
 */
data class WeightedVote(
    val sensorName: String,
    val vote: SensorVote,
    val confidence: Float,
    val weight: Float
) {
    // Puntuación efectiva de este voto
    // Un voto con 50% confianza y peso 0.35 vale menos
    // que uno con 90% confianza y peso 0.35
    val effectiveScore: Float
        get() = confidence * weight
}
// ════════════════════════════════════════════════════
// 5. RESULTADO FINAL DE LA FUSIÓN
// Lo que devuelve el SensorFusionEngine después
// de combinar los votos de los 4 sensores
// ════════════════════════════════════════════════════

/**
 * El resultado final completo del análisis post-alarma.
 * Contiene el estado detectado, la confianza global,
 * los votos individuales de cada sensor y todas
 * las métricas para debugging y pattern learning.
 */

data class PostAlarmAnalysis(

    // ── Resultado principal ──────────────────────────
    val state: PostAlarmState,          // Estado final detectado
    val confidence: Float,              // Confianza global 0.0 → 1.0
    val isReliable: Boolean,            // True si confianza > 0.7

    // ── Votos individuales ───────────────────────────
    val accelerometerVote: WeightedVote,
    val gyroscopeVote: WeightedVote,
    val microphoneVote: WeightedVote,
    val lightVote: WeightedVote,

    // ── Resultados detallados de cada sensor ─────────
    val accelerometerResult: AccelerometerResult,
    val gyroscopeResult: GyroscopeResult,
    val microphoneResult: MicrophoneResult,
    val lightResult: LightResult,

    // ── Metadata del análisis ────────────────────────
    val startTimestamp: Long,
    val endTimestamp: Long,
    val analysisDurationMs: Long,
    val weightsUsed: SensorWeights

) {
    // ── Propiedades calculadas ───────────────────────

    // Cuántos sensores votaron con certeza (no UNCERTAIN)
    val sensorsWithClearVote: Int
        get() = listOf(
            accelerometerVote.vote,
            gyroscopeVote.vote,
            microphoneVote.vote,
            lightVote.vote
        ).count { it != SensorVote.UNCERTAIN }

    // Cuántos sensores están de acuerdo con el resultado final
    val sensorsAgreeing: Int
        get() {
            val expectedVote = when (state) {
                PostAlarmState.UP_WITH_PHONE,
                PostAlarmState.UP_WITHOUT_PHONE -> SensorVote.CLEARLY_UP
                PostAlarmState.IN_BED_WITH_PHONE,
                PostAlarmState.IN_BED_WITHOUT_PHONE -> SensorVote.CLEARLY_IN_BED
                PostAlarmState.UNKNOWN -> SensorVote.UNCERTAIN
            }
            return listOf(
                accelerometerVote.vote,
                gyroscopeVote.vote,
                microphoneVote.vote,
                lightVote.vote
            ).count { it == expectedVote }
        }

    // Nivel de acuerdo entre sensores (0.0 → 1.0)
    val sensorAgreementRatio: Float
        get() = sensorsAgreeing / 4f

    // El sensor que votó con más confianza
    val mostConfidentSensor: WeightedVote
        get() = listOf(
            accelerometerVote,
            gyroscopeVote,
            microphoneVote,
            lightVote
        ).maxByOrNull { it.confidence } ?: accelerometerVote

    // El sensor que más influyó en la decisión final
    val mostInfluentialSensor: WeightedVote
        get() = listOf(
            accelerometerVote,
            gyroscopeVote,
            microphoneVote,
            lightVote
        ).maxByOrNull { it.effectiveScore } ?: accelerometerVote

    // ── Resumen legible ──────────────────────────────

    val summary: String
        get() = """
            ╔══════════════════════════════════════╗
            KOVA — ANÁLISIS POST-ALARMA
            ╚══════════════════════════════════════╝
            
            Estado detectado: $state
            Confianza global: ${(confidence * 100).toInt()}%
            Fiable: $isReliable
            Sensores de acuerdo: $sensorsAgreeing/4
            Duración análisis: ${analysisDurationMs / 1000}s
            
            ── Votos ──────────────────────────────
            Acelerómetro [${(weightsUsed.accelerometer * 100).toInt()}%]:
              ${accelerometerVote.vote} — ${(accelerometerVote.confidence * 100).toInt()}% confianza
            
            Giroscopio [${(weightsUsed.gyroscope * 100).toInt()}%]:
              ${gyroscopeVote.vote} — ${(gyroscopeVote.confidence * 100).toInt()}% confianza
            
            Micrófono [${(weightsUsed.microphone * 100).toInt()}%]:
              ${microphoneVote.vote} — ${(microphoneVote.confidence * 100).toInt()}% confianza
            
            Luz [${(weightsUsed.light * 100).toInt()}%]:
              ${lightVote.vote} — ${(lightVote.confidence * 100).toInt()}% confianza
            
            ── Sensor más influyente ───────────────
            ${mostInfluentialSensor.sensorName}
            
            ── Detalle por sensor ─────────────────
            ${accelerometerResult.summary}
            
            ${gyroscopeResult.summary}
            
            ${microphoneResult.summary}
            
            ${lightResult.summary}
        """.trimIndent()

    // ── Datos para pattern learning ──────────────────
    // Estos datos se guardarán en el historial del usuario
    // para que Kova aprenda su patrón matutino

    val patternData: Map<String, Any>
        get() = mapOf(
            "state" to state.name,
            "confidence" to confidence,
            "timestamp" to startTimestamp,
            "accel_movement" to accelerometerResult.averageNetMovement,
            "accel_variance" to accelerometerResult.variance,
            "accel_flat_ratio" to accelerometerResult.flatTimeRatio,
            "accel_steps" to accelerometerResult.stepCount,
            "gyro_rotation" to gyroscopeResult.averageRotationRate,
            "gyro_stable_pct" to gyroscopeResult.stablePercentage,
            "gyro_wrist" to gyroscopeResult.wristMovementDetected,
            "mic_db" to microphoneResult.averageDb,
            "mic_silent_ratio" to microphoneResult.silentPeriodRatio,
            "mic_footsteps" to microphoneResult.footstepsDetected,
            "light_lux" to lightResult.averageLux,
            "light_changes" to lightResult.significantChanges,
            "light_turned_on" to lightResult.lightTurnedOn,
            "sensors_agreeing" to sensorsAgreeing,
            "analysis_duration_ms" to analysisDurationMs
        )
}