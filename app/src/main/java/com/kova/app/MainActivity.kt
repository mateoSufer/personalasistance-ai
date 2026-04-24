package com.kova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.kova.app.ui.theme.KovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KovaTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf("welcome") }
    var userName by remember { mutableStateOf("") }
    var userGoal by remember { mutableStateOf("") }

    when (currentScreen) {
        "welcome" -> WelcomeScreen(
            onStart = { currentScreen = "onboarding" }
        )
        "onboarding" -> OnboardingScreen(
            onFinish = { name, goal ->
                userName = name
                userGoal = goal
                currentScreen = "home"
            }
        )
        "home" -> HomeScreen(
            userName = userName,
            userGoal = userGoal
        )
    }
}

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                )
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
                placeholder = { Text("e.g. Pass the police exam", color = Color(0xFF555555)) },
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
                onClick = {
                    if (name.isNotBlank() && goal.isNotBlank()) {
                        onFinish(name, goal)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF)
                ),
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

@Composable
fun HomeScreen(userName: String, userGoal: String) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kova is watching.\nI'll alert you when you get distracted.",
                fontSize = 14.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}