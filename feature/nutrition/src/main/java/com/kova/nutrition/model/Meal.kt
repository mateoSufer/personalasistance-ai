package com.kova.nutrition.model

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

enum class InputMethod {
    TEXT,
    PHOTO,
    VOICE
}

data class Meal(
    val id: Long = 0,
    val date: String,
    val timeHour: Int,
    val description: String,
    val inputMethod: InputMethod,
    val mealType: MealType,
    val estimatedCalories: Int,
    val estimatedProteinG: Float,
    val estimatedCarbsG: Float,
    val estimatedFatG: Float,
    val confidenceNote: String = ""
)
