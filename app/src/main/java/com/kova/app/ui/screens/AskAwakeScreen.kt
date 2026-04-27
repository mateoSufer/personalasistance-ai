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

@Composable
fun AskAwakeScreen(
    userName: String,
    onConfirmAwake: () -> Unit,
    onSnooze: () -> Unit
) {
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
            Text(text = "🤔", fontSize = 52.sp, textAlign = TextAlign.Center)
            Text(
                text = "$userName, ¿estás levantado?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kova no pudo determinarlo\npor los sensores.",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirmAwake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Text(
                    text = "Sí, estoy en pie",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text(
                    text = "Dame 5 minutos más",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
