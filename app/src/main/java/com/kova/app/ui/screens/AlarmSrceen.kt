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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(
    userName: String,
    userGoal: String,
    daysLeft: Int,
    onAwake: () -> Unit,
    onSnooze: () -> Unit,
    snoozeCount: Int,
    maxSnooze: Int
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = currentTime,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                letterSpacing = 4.sp
            )

            Text(
                text = "Buenos días, $userName.",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Objetivo: $userGoal",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Te quedan $daysLeft días para el examen.",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAwake,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
            ) {
                Text(
                    text = "✓ Estoy despierto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }

            if (snoozeCount < maxSnooze) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF666666),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color(0xFF444444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFF333333)
                    )
                ) {
                    Text(
                        text = "5 minutos más (${maxSnooze - snoozeCount} restantes)",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                Text(
                    text = "No quedan snoozes. Es hora de levantarse.",
                    fontSize = 13.sp,
                    color = Color(0xFFE24B4A),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cada mañana cuenta.",
                fontSize = 12.sp,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center
            )
        }
    }
}