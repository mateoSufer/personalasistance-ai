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
fun WelcomeScreen(onStart: () -> Unit) {
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
                text = "KOVA",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                letterSpacing = 8.sp
            )
            Text(
                text = "Your personal focus coach.\nWhen you get distracted, I'll let you know.",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Text(
                    text = "Get started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}
