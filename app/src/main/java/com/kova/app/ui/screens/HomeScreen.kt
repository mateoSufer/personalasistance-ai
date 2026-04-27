package com.kova.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.kova.app.ui.screens.MorningCheckIn

@Composable
fun HomeScreen(
    userName: String,
    userGoal: String,
    morningCheckIn: MorningCheckIn?,
    onOpenAlarmSettings: () -> Unit = {},
    onOpenTest: () -> Unit = {},
    onOpenNutrition: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Ready, $userName.",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Goal: $userGoal",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
            morningCheckIn?.let { checkIn ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${checkIn.energyLevel.emoji} ${checkIn.energyLevel.label} · ${checkIn.sleepQuality.emoji} ${checkIn.sleepQuality.label}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Estudio previsto a las ${String.format("%02d", checkIn.studyStartHour)}:00",
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kova is watching.\nI'll alert you when you get distracted.",
                fontSize = 14.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenAlarmSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(text = "⏰ Configurar alarma", fontSize = 14.sp, color = Color(0xFF9E9E9E))
            }
            OutlinedButton(
                onClick = onOpenNutrition,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(text = "🥗 Nutrición", fontSize = 14.sp, color = Color(0xFF9E9E9E))
            }
            OutlinedButton(
                onClick = onOpenTest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(text = "🔬 Test de sensores", fontSize = 14.sp, color = Color(0xFF9E9E9E))
            }
        }
    }
}
