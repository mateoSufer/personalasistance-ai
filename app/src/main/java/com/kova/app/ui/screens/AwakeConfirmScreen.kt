package com.kova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kova.app.domain.model.PostAlarmState
import kotlinx.coroutines.delay

@Composable
fun AwakeConfirmScreen(
    userName: String,
    onStateDetected: (PostAlarmState) -> Unit,
    onAskUser: () -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Verificando...") }
    var progress by remember { mutableFloatStateOf(0f) }
    val totalSeconds = 600 // 10 minutos

    LaunchedEffect(Unit) {
        while (elapsedSeconds < totalSeconds) {
            delay(1000)
            elapsedSeconds++
            progress = elapsedSeconds / totalSeconds.toFloat()

            statusMessage = when {
                elapsedSeconds < 60 -> "Observando tu movimiento..."
                elapsedSeconds < 180 -> "Analizando señales..."
                elapsedSeconds < 360 -> "Detectando si estás levantado..."
                elapsedSeconds < 540 -> "Casi listo..."
                else -> "Finalizando análisis..."
            }
        }
        // Si después de 10 minutos no hay señal clara, pregunta
        onAskUser()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔍",
                fontSize = 52.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Verificando, $userName...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )

            Text(
                text = statusMessage,
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFFFFFFFF),
                trackColor = Color(0xFF333333)
            )

            Text(
                text = "${elapsedSeconds / 60}:${String.format("%02d", elapsedSeconds % 60)}",
                fontSize = 13.sp,
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Kova está analizando tu movimiento\npara saber si realmente te levantaste.",
                fontSize = 12.sp,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // El usuario puede confirmar manualmente si quiere
            OutlinedButton(
                onClick = { onStateDetected(PostAlarmState.UP_WITH_PHONE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFF333333)
                )
            ) {
                Text(
                    text = "Sí, ya estoy levantado",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}