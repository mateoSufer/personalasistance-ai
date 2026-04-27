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
fun AlarmSettingsScreen(
    userName: String,
    currentHour: Int,
    currentMinute: Int,
    currentMaxSnooze: Int,
    onSaveAlarm: (hour: Int, minute: Int, maxSnooze: Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(currentHour) }
    var selectedMinute by remember { mutableIntStateOf(currentMinute) }
    var selectedMaxSnooze by remember { mutableIntStateOf(currentMaxSnooze) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Alarma",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )

            Text(
                text = "Configura tu hora de despertar, $userName.",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hora
            Text(
                text = "Hora de despertar",
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Horas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Hora",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                selectedHour = if (selectedHour == 0) 23
                                else selectedHour - 1
                            }
                        ) {
                            Text("−", fontSize = 24.sp, color = Color(0xFFFFFFFF))
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF)
                        )
                        IconButton(
                            onClick = {
                                selectedHour = if (selectedHour == 23) 0
                                else selectedHour + 1
                            }
                        ) {
                            Text("+", fontSize = 24.sp, color = Color(0xFFFFFFFF))
                        }
                    }
                }

                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )

                // Minutos
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Minutos",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                selectedMinute = if (selectedMinute == 0) 59
                                else selectedMinute - 5
                            }
                        ) {
                            Text("−", fontSize = 24.sp, color = Color(0xFFFFFFFF))
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF)
                        )
                        IconButton(
                            onClick = {
                                selectedMinute = if (selectedMinute >= 55) 0
                                else selectedMinute + 5
                            }
                        ) {
                            Text("+", fontSize = 24.sp, color = Color(0xFFFFFFFF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Snooze
            Text(
                text = "Snoozes permitidos",
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0, 1, 2).forEach { snoozeOption ->
                    val isSelected = selectedMaxSnooze == snoozeOption
                    OutlinedButton(
                        onClick = { selectedMaxSnooze = snoozeOption },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color(0xFFFFFFFF)
                            else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFFFFFF) else Color(0xFF333333)
                        )
                    ) {
                        Text(
                            text = snoozeOption.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF0D0D0D)
                            else Color(0xFF666666)
                        )
                    }
                }
            }

            Text(
                text = when (selectedMaxSnooze) {
                    0 -> "Sin piedad. La alarma no para hasta que te levantes."
                    1 -> "Un margen. Solo uno."
                    2 -> "Dos oportunidades. No abuses."
                    else -> ""
                },
                fontSize = 12.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSaveAlarm(selectedHour, selectedMinute, selectedMaxSnooze)
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
                    text = "Guardar alarma — ${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)}",
                    fontSize = 16.sp,
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
                    text = "Volver",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}