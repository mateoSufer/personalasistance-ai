package com.kova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onFinish: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

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
            Text(
                text = "Let's get to know you",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kova needs to know your goal\nto keep you on track.",
                fontSize = 15.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name", color = Color(0xFF9E9E9E)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFFFFF),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Your main goal", color = Color(0xFF9E9E9E)) },
                placeholder = {
                    Text("e.g. Pass the police exam", color = Color(0xFF555555))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFFFFF),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { if (name.isNotBlank() && goal.isNotBlank()) onFinish(name, goal) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF)),
                enabled = name.isNotBlank() && goal.isNotBlank()
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D0D0D)
                )
            }
        }
    }
}
