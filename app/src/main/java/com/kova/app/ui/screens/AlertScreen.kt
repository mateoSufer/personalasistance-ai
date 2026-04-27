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

@Composable
fun AlertScreen(
    userName: String,
    userGoal: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "⚠️", fontSize = 52.sp, textAlign = TextAlign.Center)
            Text(
                text = "$userName, you're getting distracted.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            Text(
                text = "Remember your goal:\n$userGoal",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Text(
                    text = "Back to work",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}
