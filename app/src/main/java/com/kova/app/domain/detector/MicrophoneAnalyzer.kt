package com.kova.app.domain.detector

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.kova.app.domain.model.MicrophoneResult
import com.kova.app.domain.model.MicrophoneSample
import com.kova.app.domain.model.SensorVote
import com.kova.app.domain.model.SoundCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class MicrophoneAnalyzer(private val context: Context) {

    private val samples = mutableListOf<MicrophoneSample>()
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var startTimestamp = 0L

    companion object {
        // â”€â”€ ConfiguraciÃ³n de audio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_MULTIPLIER = 4

        // â”€â”€ Umbrales de sonido (dB) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        // Por debajo â†’ silencio absoluto
        const val SILENCE_THRESHOLD_DB = 20f

        // PatrÃ³n de pasos â€” picos rÃ­tmicos de sonido
        const val FOOTSTEP_MIN_DB = 35f
        const val FOOTSTEP_MAX_DB = 65f
        const val FOOTSTEP_MIN_INTERVAL_MS = 300L
        const val FOOTSTEP_MAX_INTERVAL_MS = 900L

        // Sonido de agua â€” ruido blanco continuo moderado
        const val WATER_MIN_DB = 40f
        const val WATER_MAX_DB = 70f
        const val WATER_VARIANCE_MAX = 8f      // Sonido constante, poca varianza

        // TV o mÃºsica â€” sonido continuo con varianza moderada
        const val TV_MIN_DB = 35f
        const val TV_VARIANCE_MIN = 5f
        const val TV_VARIANCE_MAX = 25f

        // Ratio de silencio para considerar que el usuario
        // estÃ¡ dormido o el mÃ³vil estÃ¡ solo
        const val SLEEPING_SILENCE_RATIO = 0.85f
    }

    // â”€â”€ Recogida de datos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun startCollecting() {
        samples.clear()
        startTimestamp = System.currentTimeMillis()

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER

        if (bufferSize <= 0) return

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            // Si el AudioRecord no se inicializÃ³ correctamente
            // no intentamos grabar â€” evita el crash
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }

            audioRecord = record
            audioRecord?.startRecording()

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ShortArray(bufferSize / 2)
                while (audioRecord?.recordingState
                    == AudioRecord.RECORDSTATE_RECORDING
                ) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val db = calculateDb(buffer, read)
                        samples.add(
                            MicrophoneSample(
                                amplitudeDb = db,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    kotlinx.coroutines.delay(200)
                }
            }
        } catch (e: SecurityException) {
            // Permiso no concedido â€” el analizador devuelve UNCERTAIN
        } catch (e: IllegalStateException) {
            // AudioRecord no inicializado â€” no crashea
            audioRecord?.release()
            audioRecord = null
        }
    }

    fun stopCollecting() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingJob = null
    }

    fun clearSamples() {
        samples.clear()
        startTimestamp = 0L
    }

    // â”€â”€ MÃ©tricas en tiempo real â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun getSampleCount(): Int = samples.size

    fun getLiveDb(): Float {
        if (samples.isEmpty()) return 0f
        return samples.last().amplitudeDb
    }

    fun getLiveCategory(): SoundCategory {
        if (samples.isEmpty()) return SoundCategory.SILENT
        return samples.last().category
    }

    // â”€â”€ AnÃ¡lisis completo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun analyze(): MicrophoneResult {
        val endTimestamp = System.currentTimeMillis()
        val duration = endTimestamp - startTimestamp

        if (samples.size < 5) {
            return buildEmptyResult(duration)
        }

        val dbValues = samples.map { it.amplitudeDb }

        // MÃ©tricas bÃ¡sicas
        val averageDb = dbValues.average().toFloat()
        val minDb = dbValues.minOrNull() ?: 0f
        val maxDb = dbValues.maxOrNull() ?: 0f
        val mean = averageDb
        val variance = dbValues
            .map { (it - mean) * (it - mean) }
            .average()
            .toFloat()

        // CategorÃ­a dominante
        val dominantCategory = samples
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key ?: SoundCategory.SILENT

        // Ratio de silencio
        val silentSamples = samples.count {
            it.amplitudeDb < SILENCE_THRESHOLD_DB
        }
        val silentPeriodRatio = silentSamples / samples.size.toFloat()

        // Patrones especÃ­ficos
        val footstepsDetected = detectFootsteps()
        val waterSoundDetected = detectWaterSound(averageDb, variance)
        val tvOrMusicDetected = detectTvOrMusic(averageDb, variance)

        // Voto y confianza
        val (vote, confidence) = determineVote(
            averageDb = averageDb,
            dominantCategory = dominantCategory,
            silentPeriodRatio = silentPeriodRatio,
            footstepsDetected = footstepsDetected,
            waterSoundDetected = waterSoundDetected,
            tvOrMusicDetected = tvOrMusicDetected,
            variance = variance
        )

        return MicrophoneResult(
            averageDb = averageDb,
            minDb = minDb,
            maxDb = maxDb,
            variance = variance,
            dominantCategory = dominantCategory,
            footstepsDetected = footstepsDetected,
            waterSoundDetected = waterSoundDetected,
            tvOrMusicDetected = tvOrMusicDetected,
            silentPeriodRatio = silentPeriodRatio,
            vote = vote,
            confidence = confidence,
            samplesCollected = samples.size,
            analysisDurationMs = duration
        )
    }

    // â”€â”€ Algoritmos internos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun calculateDb(buffer: ShortArray, read: Int): Float {
        // Calcula el nivel de presiÃ³n sonora en dB
        // a partir del buffer de audio PCM
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i].toDouble()
        }
        val rms = sqrt(sum / read)
        return if (rms > 0) {
            (20 * log10(rms)).toFloat().coerceIn(0f, 120f)
        } else {
            0f
        }
    }

    private fun detectFootsteps(): Boolean {
        // Los pasos generan picos rÃ­tmicos de sonido
        // en el rango de frecuencia de impacto en el suelo
        // Detectamos picos que ocurren con intervalo regular
        val peaks = mutableListOf<Long>()
        var inPeak = false

        for (sample in samples) {
            if (sample.amplitudeDb in FOOTSTEP_MIN_DB..FOOTSTEP_MAX_DB) {
                if (!inPeak) {
                    peaks.add(sample.timestamp)
                    inPeak = true
                }
            } else {
                inPeak = false
            }
        }

        if (peaks.size < 3) return false

        // Verifica que los picos son rÃ­tmicos
        var rhythmicPeaks = 0
        for (i in 1 until peaks.size) {
            val interval = peaks[i] - peaks[i - 1]
            if (interval in FOOTSTEP_MIN_INTERVAL_MS..FOOTSTEP_MAX_INTERVAL_MS) {
                rhythmicPeaks++
            }
        }

        // Al menos el 60% de los picos son rÃ­tmicos
        return rhythmicPeaks >= peaks.size * 0.6f
    }

    private fun detectWaterSound(
        averageDb: Float,
        variance: Float
    ): Boolean {
        // El agua genera un ruido blanco continuo
        // con nivel moderado y muy poca varianza
        return averageDb in WATER_MIN_DB..WATER_MAX_DB
                && variance < WATER_VARIANCE_MAX
    }

    private fun detectTvOrMusic(
        averageDb: Float,
        variance: Float
    ): Boolean {
        // TV o mÃºsica: sonido continuo con varianza moderada
        // No es silencio pero tampoco es ruido de actividad
        return averageDb > TV_MIN_DB
                && variance in TV_VARIANCE_MIN..TV_VARIANCE_MAX
    }

    private fun determineVote(
        averageDb: Float,
        dominantCategory: SoundCategory,
        silentPeriodRatio: Float,
        footstepsDetected: Boolean,
        waterSoundDetected: Boolean,
        tvOrMusicDetected: Boolean,
        variance: Float
    ): Pair<SensorVote, Float> {

        // â”€â”€ Caso 1: Pasos detectados â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // SeÃ±al muy fuerte â€” el usuario estÃ¡ caminando
        if (footstepsDetected) {
            return Pair(SensorVote.CLEARLY_UP, 0.85f)
        }

        // â”€â”€ Caso 2: Sonido de agua â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Ducha, lavarse â€” claramente levantado
        if (waterSoundDetected) {
            return Pair(SensorVote.CLEARLY_UP, 0.80f)
        }

        // â”€â”€ Caso 3: Silencio casi total â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Dormido o mÃ³vil solo â€” sin actividad sonora
        if (silentPeriodRatio > SLEEPING_SILENCE_RATIO) {
            val confidence = silentPeriodRatio * 0.7f
            return Pair(SensorVote.CLEARLY_IN_BED, confidence)
        }

        // â”€â”€ Caso 4: TV o mÃºsica â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Entretenimiento â€” probablemente tumbado
        if (tvOrMusicDetected) {
            return Pair(SensorVote.CLEARLY_IN_BED, 0.60f)
        }

        // â”€â”€ Caso 5: Sonido activo sin patrÃ³n claro â”€â”€â”€â”€
        // Hay ruido de actividad pero no identificable
        if (dominantCategory == SoundCategory.NORMAL
            || dominantCategory == SoundCategory.LOUD
        ) {
            return Pair(SensorVote.CLEARLY_UP, 0.50f)
        }

        // â”€â”€ Caso por defecto â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        return Pair(SensorVote.UNCERTAIN, 0.2f)
    }

    fun buildEmptyResult(duration: Long): MicrophoneResult {
        return MicrophoneResult(
            averageDb = 0f,
            minDb = 0f,
            maxDb = 0f,
            variance = 0f,
            dominantCategory = SoundCategory.SILENT,
            footstepsDetected = false,
            waterSoundDetected = false,
            tvOrMusicDetected = false,
            silentPeriodRatio = 1f,
            vote = SensorVote.UNCERTAIN,
            confidence = 0f,
            samplesCollected = 0,
            analysisDurationMs = duration
        )
    }
}

