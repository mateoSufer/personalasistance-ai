package com.kova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class MorningCheckIn(
    val sleepQuality: SleepQuality,
    val energyLevel: EnergyLevel,
    val studyStartHour: Int
)

enum class SleepQuality(val emoji: String, val label: String) {
    BADLY("😴", "Mal"),
    OK("😐", "Regular"),
    WELL("😊", "Bien")
}

enum class EnergyLevel(val emoji: String, val label: String, val color: Color) {
    LOW("🔴", "Sin energía", Color(0xFFE24B4A)),
    NORMAL("🟡", "Normal", Color(0xFFBA7517)),
    HIGH("🟢", "Con energía", Color(0xFF1D9E75))
}

@Composable
fun MorningCheckInScreen(
    userName: String,
    onComplete: (MorningCheckIn) -> Unit
) {
    var selectedSleep by remember { mutableStateOf<SleepQuality?>(null) }
    var selectedEnergy by remember { mutableStateOf<EnergyLevel?>(null) }
    var selectedHour by remember { mutableIntStateOf(9) }
    var currentStep by remember { mutableIntStateOf(1) }

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
                text = "Buenos días, $userName.",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )

            // Indicador de paso
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3).forEach { step ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (step <= currentStep) Color(0xFFFFFFFF)
                                else Color(0xFF333333)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (currentStep) {

                1 -> {
                    Text(
                        text = "¿Cómo dormiste?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SleepQuality.entries.forEach { quality ->
                            val isSelected = selectedSleep == quality
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color(0xFF1A1A1A)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) Color(0xFFFFFFFF)
                                        else Color(0xFF333333),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedSleep = quality }
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = quality.emoji,
                                    fontSize = 36.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = quality.label,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFFFFFFFF)
                                    else Color(0xFF666666)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { if (selectedSleep != null) currentStep = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSleep != null)
                                Color(0xFFFFFFFF) else Color(0xFF222222)
                        ),
                        enabled = selectedSleep != null
                    ) {
                        Text(
                            text = "Siguiente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSleep != null)
                                Color(0xFF0D0D0D) else Color(0xFF555555)
                        )
                    }
                }

                2 -> {
                    Text(
                        text = "¿Cómo te encuentras?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EnergyLevel.entries.forEach { energy ->
                            val isSelected = selectedEnergy == energy
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color(0xFF1A1A1A)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) energy.color
                                        else Color(0xFF333333),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedEnergy = energy }
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = energy.emoji,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = energy.label,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFFFFFF)
                                    else Color(0xFF666666)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { if (selectedEnergy != null) currentStep = 3 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedEnergy != null)
                                Color(0xFFFFFFFF) else Color(0xFF222222)
                        ),
                        enabled = selectedEnergy != null
                    ) {
                        Text(
                            text = "Siguiente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedEnergy != null)
                                Color(0xFF0D0D0D) else Color(0xFF555555)
                        )
                    }
                }

                3 -> {
                    Text(
                        text = "¿A qué hora empiezas a estudiar?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Kova te recordará a esa hora.",
                        fontSize = 14.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                selectedHour = if (selectedHour == 5) 22
                                else selectedHour - 1
                            }
                        ) {
                            Text("−", fontSize = 28.sp, color = Color(0xFFFFFFFF))
                        }

                        Text(
                            text = String.format("%02d:00", selectedHour),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        IconButton(
                            onClick = {
                                selectedHour = if (selectedHour == 22) 5
                                else selectedHour + 1
                            }
                        ) {
                            Text("+", fontSize = 28.sp, color = Color(0xFFFFFFFF))
                        }
                    }

                    // Mensaje adaptado al estado del usuario
                    val adaptiveMessage = when {
                        selectedSleep == SleepQuality.BADLY &&
                                selectedEnergy == EnergyLevel.LOW ->
                            "Dormiste mal y tienes poca energía. Objetivo reducido hoy: 2.5h. Empieza con el tema más fácil."
                        selectedSleep == SleepQuality.WELL &&
                                selectedEnergy == EnergyLevel.HIGH ->
                            "Estás en tu mejor momento. Objetivo completo: 4h. Ataca el bloque más difícil."
                        selectedEnergy == EnergyLevel.LOW ->
                            "Energía baja. Objetivo reducido: 3h. No te exijas demasiado hoy."
                        selectedEnergy == EnergyLevel.HIGH ->
                            "Buena energía. Aprovéchala. Objetivo: 4h."
                        else ->
                            "Día normal. Objetivo: 3.5h. Mantén el ritmo."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = adaptiveMessage,
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            onComplete(
                                MorningCheckIn(
                                    sleepQuality = selectedSleep!!,
                                    energyLevel = selectedEnergy!!,
                                    studyStartHour = selectedHour
                                )
                            )
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
                            text = "Empezar el día",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D0D0D)
                        )
                    }
                }
            }
        }
    }
}