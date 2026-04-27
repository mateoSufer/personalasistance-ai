package com.kova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kova.app.domain.detector.SensorFusionEngine
import com.kova.app.domain.model.PostAlarmAnalysis
import com.kova.app.domain.model.PostAlarmState
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun AccelerometerTestScreen(
    fusionEngine: SensorFusionEngine,
    detector: com.kova.app.domain.detector.DistractionDetector,
    onBack: () -> Unit
) {
    var phase by remember { mutableStateOf("ready") }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var liveSensorData by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var analysis by remember { mutableStateOf<PostAlarmAnalysis?>(null) }
    var userConfirmed by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val testDurationSeconds = 60

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(phase) {
        if (phase == "testing") {
            fusionEngine.clearAll()
                    fusionEngine.startCollecting()
                    detector.isPaused = true

            // Actualiza mÃ©tricas en tiempo real
            scope.launch {
                while (phase == "testing") {
                    kotlinx.coroutines.delay(500)
                    liveSensorData = fusionEngine.getLiveSensorData()
                }
            }

            // Cuenta atrÃ¡s
            elapsedSeconds = 0
            while (elapsedSeconds < testDurationSeconds) {
                kotlinx.coroutines.delay(1000)
                elapsedSeconds++
            }

            // AnÃ¡lisis final
            val result = fusionEngine.analyze()
            fusionEngine.stopCollecting()
                detector.isPaused = false
                analysis = result
                phase = "result"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            when (phase) {

                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                // FASE 1 â€” Listo para empezar
                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                "ready" -> {
                    Text(
                        text = "ðŸ”¬",
                        fontSize = 52.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Test de sensores",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Kova va a observarte durante 1 minuto\ny adivinar quÃ© estÃ¡s haciendo.",
                        fontSize = 14.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instrucciones de los 4 escenarios
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(18.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Puedes hacer cualquiera de estas cosas:",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                            ScenarioHint(
                                color = Color(0xFF1D9E75),
                                text = "Dejar el mÃ³vil en la mesilla y levantarte"
                            )
                            ScenarioHint(
                                color = Color(0xFFBA7517),
                                text = "Quedarte tumbado con el mÃ³vil sobre ti sin usarlo"
                            )
                            ScenarioHint(
                                color = Color(0xFF378ADD),
                                text = "Levantarte y caminar llevando el mÃ³vil"
                            )
                            ScenarioHint(
                                color = Color(0xFFE24B4A),
                                text = "Quedarte tumbado usando el mÃ³vil (scroll, apps)"
                            )
                            Text(
                                text = "Kova intentarÃ¡ adivinar cuÃ¡l hiciste.",
                                fontSize = 12.sp,
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                phase = "testing"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFFFFF)
                        )
                    ) {
                        Text(
                            text = when {
                                !hasMicPermission -> "ðŸŽ™ï¸ Conceder permiso de micrÃ³fono"
                                else -> "â–¶ Iniciar test"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D0D0D)
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFF333333)
                        )
                    ) {
                        Text("Volver", fontSize = 14.sp, color = Color(0xFF666666))
                    }
                }

                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                // FASE 2 â€” Midiendo
                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                "testing" -> {
                    val remaining = testDurationSeconds - elapsedSeconds
                    val progress = elapsedSeconds / testDurationSeconds.toFloat()

                    Text(
                        text = "Kova te estÃ¡ observando...",
                        fontSize = 16.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )

                    // Cuenta atrÃ¡s grande
                    Text(
                        text = remaining.toString(),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            remaining > 30 -> Color(0xFFFFFFFF)
                            remaining > 10 -> Color(0xFFBA7517)
                            else -> Color(0xFFE24B4A)
                        }
                    )

                    Text(
                        text = "segundos",
                        fontSize = 14.sp,
                        color = Color(0xFF555555)
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFFFFFFFF),
                        trackColor = Color(0xFF333333)
                    )

                    Text(
                        text = "Haz lo que vayas a hacer.\nNo mires la pantalla.",
                        fontSize = 13.sp,
                        color = Color(0xFF444444),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // MÃ©tricas en tiempo real
                    Text(
                        text = "SENSORES ACTIVOS",
                        fontSize = 10.sp,
                        color = Color(0xFF333333),
                        letterSpacing = 2.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensorLiveBox(
                            label = "Movimiento",
                            value = "%.2f".format(
                                liveSensorData["accel_movement"] ?: 0f
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        SensorLiveBox(
                            label = "RotaciÃ³n",
                            value = "%.2f".format(
                                liveSensorData["gyro_rotation"] ?: 0f
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensorLiveBox(
                            label = "Luz (lux)",
                            value = "%.0f".format(
                                liveSensorData["light_lux"] ?: 0f
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        SensorLiveBox(
                            label = "Sonido (dB)",
                            value = "%.0f".format(
                                liveSensorData["mic_db"] ?: 0f
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                // FASE 3 â€” Resultado
                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                "result" -> {
                    analysis?.let { result ->

                        Text(
                            text = "Kova cree que estabas...",
                            fontSize = 15.sp,
                            color = Color(0xFF9E9E9E),
                            textAlign = TextAlign.Center
                        )

                        // Estado detectado en grande
                        val stateEmoji = when (result.state) {
                            PostAlarmState.UP_WITH_PHONE -> "ðŸš¶ðŸ“±"
                            PostAlarmState.UP_WITHOUT_PHONE -> "ðŸš¶"
                            PostAlarmState.IN_BED_WITH_PHONE -> "ðŸ›ŒðŸ“±"
                            PostAlarmState.IN_BED_WITHOUT_PHONE -> "ðŸ›Œ"
                            PostAlarmState.UNKNOWN -> "ðŸ¤”"
                        }

                        val stateLabel = when (result.state) {
                            PostAlarmState.UP_WITH_PHONE ->
                                "Levantado\ncon el mÃ³vil"
                            PostAlarmState.UP_WITHOUT_PHONE ->
                                "Levantado\nsin el mÃ³vil"
                            PostAlarmState.IN_BED_WITH_PHONE ->
                                "En cama\nusando el mÃ³vil"
                            PostAlarmState.IN_BED_WITHOUT_PHONE ->
                                "En cama\nsin usar el mÃ³vil"
                            PostAlarmState.UNKNOWN ->
                                "No pude\ndeterminarlo"
                        }

                        val stateColor = when (result.state) {
                            PostAlarmState.UP_WITH_PHONE -> Color(0xFF378ADD)
                            PostAlarmState.UP_WITHOUT_PHONE -> Color(0xFF1D9E75)
                            PostAlarmState.IN_BED_WITH_PHONE -> Color(0xFFE24B4A)
                            PostAlarmState.IN_BED_WITHOUT_PHONE -> Color(0xFFBA7517)
                            PostAlarmState.UNKNOWN -> Color(0xFF666666)
                        }

                        Text(
                            text = stateEmoji,
                            fontSize = 64.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stateLabel,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = stateColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )

                        // Confianza
                        Text(
                            text = "Confianza: ${(result.confidence * 100).toInt()}%",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E),
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { result.confidence },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = stateColor,
                            trackColor = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pregunta de confirmaciÃ³n
                        if (userConfirmed == null) {
                            Text(
                                text = "Â¿AcertÃ³ Kova?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { userConfirmed = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1D9E75)
                                    )
                                ) {
                                    Text(
                                        text = "âœ… SÃ­",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                                Button(
                                    onClick = { userConfirmed = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE24B4A)
                                    )
                                ) {
                                    Text(
                                        text = "âŒ No",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                            }
                        } else {
                            // Feedback tras confirmar
                            Text(
                                text = if (userConfirmed == true)
                                    "âœ… Kova acertÃ³. Los sensores funcionan correctamente."
                                else
                                    "âŒ Kova fallÃ³. Este resultado ayuda a mejorar los umbrales de detecciÃ³n.",
                                fontSize = 13.sp,
                                color = if (userConfirmed == true)
                                    Color(0xFF1D9E75) else Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Votos de cada sensor
                        Text(
                            text = "CÃ“MO VOTÃ“ CADA SENSOR",
                            fontSize = 10.sp,
                            color = Color(0xFF333333),
                            letterSpacing = 2.sp
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1A))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SensorVoteRow(
                                emoji = "ðŸ“³",
                                sensor = "AcelerÃ³metro",
                                vote = result.accelerometerVote.vote.name,
                                confidence = result.accelerometerVote.confidence,
                                weight = result.accelerometerVote.weight
                            )
                            SensorVoteRow(
                                emoji = "ðŸŒ€",
                                sensor = "Giroscopio",
                                vote = result.gyroscopeVote.vote.name,
                                confidence = result.gyroscopeVote.confidence,
                                weight = result.gyroscopeVote.weight
                            )
                            SensorVoteRow(
                                emoji = "ðŸŽ™ï¸",
                                sensor = "MicrÃ³fono",
                                vote = result.microphoneVote.vote.name,
                                confidence = result.microphoneVote.confidence,
                                weight = result.microphoneVote.weight
                            )
                            SensorVoteRow(
                                emoji = "ðŸ’¡",
                                sensor = "Luz",
                                vote = result.lightVote.vote.name,
                                confidence = result.lightVote.confidence,
                                weight = result.lightVote.weight
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // MÃ©tricas detalladas
                        Text(
                            text = "MÃ‰TRICAS DETALLADAS",
                            fontSize = 10.sp,
                            color = Color(0xFF333333),
                            letterSpacing = 2.sp
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1A))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricRow("Movimiento promedio",
                                "%.4f".format(
                                    result.accelerometerResult.averageNetMovement
                                ))
                            MetricRow("Varianza accel",
                                "%.5f".format(result.accelerometerResult.variance))
                            MetricRow("Pasos detectados",
                                result.accelerometerResult.stepCount.toString())
                            MetricRow("% tiempo caminando",
                                "${"%.1f".format(
                                    result.accelerometerResult.walkingTimeRatio * 100
                                )}%")
                            MetricRow("Periodo quieto mÃ¡s largo",
                                "${result.accelerometerResult.longestFlatPeriodMs / 1000}s")
                            HorizontalDivider(
                                color = Color(0xFF2A2A2A),
                                thickness = 0.5.dp
                            )
                            MetricRow("RotaciÃ³n promedio",
                                "%.4f".format(
                                    result.gyroscopeResult.averageRotationRate
                                ))
                            MetricRow("% tiempo estable",
                                "${"%.1f".format(
                                    result.gyroscopeResult.stablePercentage * 100
                                )}%")
                            MetricRow("Movimiento de muÃ±eca",
                                if (result.gyroscopeResult.wristMovementDetected)
                                    "SÃ­" else "No")
                            MetricRow("Cambios de postura",
                                result.gyroscopeResult.positionChanges.toString())
                            HorizontalDivider(
                                color = Color(0xFF2A2A2A),
                                thickness = 0.5.dp
                            )
                            MetricRow("Volumen promedio",
                                "${"%.1f".format(
                                    result.microphoneResult.averageDb
                                )} dB")
                            MetricRow("% tiempo silencio",
                                "${"%.1f".format(
                                    result.microphoneResult.silentPeriodRatio * 100
                                )}%")
                            MetricRow("Pasos por sonido",
                                if (result.microphoneResult.footstepsDetected)
                                    "SÃ­" else "No")
                            MetricRow("Agua detectada",
                                if (result.microphoneResult.waterSoundDetected)
                                    "SÃ­" else "No")
                            HorizontalDivider(
                                color = Color(0xFF2A2A2A),
                                thickness = 0.5.dp
                            )
                            MetricRow("Luz promedio",
                                "${"%.1f".format(result.lightResult.averageLux)} lux")
                            MetricRow("EncendiÃ³ la luz",
                                if (result.lightResult.lightTurnedOn) "SÃ­" else "No")
                            MetricRow("CambiÃ³ de habitaciÃ³n",
                                if (result.lightResult.movedToNewRoom) "SÃ­" else "No")
                            MetricRow("Cambios de luz",
                                result.lightResult.significantChanges.toString())
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                analysis = null
                                userConfirmed = null
                                elapsedSeconds = 0
                                phase = "ready"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFFFFF)
                            )
                        ) {
                            Text(
                                text = "Repetir test",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D0D0D)
                            )
                        }

                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF333333)
                            )
                        ) {
                            Text(
                                "Volver",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// â”€â”€ Componentes auxiliares â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun ScenarioHint(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun SensorLiveBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .padding(12.dp)
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFFFFF)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF555555)
        )
    }
}

@Composable
fun SensorVoteRow(
    emoji: String,
    sensor: String,
    vote: String,
    confidence: Float,
    weight: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Column {
                Text(
                    text = sensor,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = vote.replace("_", " "),
                    fontSize = 11.sp,
                    color = Color(0xFF555555)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${(confidence * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )
            Text(
                text = "peso ${(weight * 100).toInt()}%",
                fontSize = 10.sp,
                color = Color(0xFF444444)
            )
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF666666))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9E9E9E)
        )
    }
}



