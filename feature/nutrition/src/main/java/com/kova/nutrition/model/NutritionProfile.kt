package com.kova.nutrition.model

enum class PhysicalGoal {
    LOSE_WEIGHT,
    MAINTAIN_WEIGHT,
    GAIN_MUSCLE
}

enum class ActivityLevel {
    SEDENTARY,
    LIGHTLY_ACTIVE,
    MODERATELY_ACTIVE,
    VERY_ACTIVE,
    EXTRA_ACTIVE
}

enum class DietaryRestriction {
    NONE,
    VEGETARIAN,
    VEGAN,
    GLUTEN_FREE,
    LACTOSE_FREE,
    HALAL
}

data class NutritionProfile(
    val ageYears: Int,
    val weightKg: Float,
    val heightCm: Float,
    val isMale: Boolean,
    val goal: PhysicalGoal,
    val activityLevel: ActivityLevel,
    val dietaryRestriction: DietaryRestriction = DietaryRestriction.NONE,
    val targetCalories: Int = 0,
    val targetProteinG: Int = 0,
    val targetCarbsG: Int = 0,
    val targetFatG: Int = 0,
    val targetWaterMl: Int = 0
)
