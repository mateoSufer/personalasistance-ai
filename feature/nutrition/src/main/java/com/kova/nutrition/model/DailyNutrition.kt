package com.kova.nutrition.model

enum class DayNutritionStatus {
    NOT_STARTED,
    LOW,
    ON_TRACK,
    COMPLETE,
    OVER
}

data class DailyNutrition(
    val date: String,
    val meals: List<Meal> = emptyList(),
    val totalCalories: Int = 0,
    val totalProteinG: Float = 0f,
    val totalCarbsG: Float = 0f,
    val totalFatG: Float = 0f,
    val totalWaterMl: Int = 0,
    val targetCalories: Int = 0,
    val targetProteinG: Int = 0,
    val targetCarbsG: Int = 0,
    val targetFatG: Int = 0,
    val targetWaterMl: Int = 0
) {
    val remainingCalories: Int get() = targetCalories - totalCalories

    val calorieProgressPct: Float
        get() = if (targetCalories == 0) 0f
                else (totalCalories.toFloat() / targetCalories).coerceIn(0f, 1f)

    val status: DayNutritionStatus
        get() = when {
            totalCalories == 0               -> DayNutritionStatus.NOT_STARTED
            calorieProgressPct < 0.4f        -> DayNutritionStatus.LOW
            calorieProgressPct < 0.9f        -> DayNutritionStatus.ON_TRACK
            calorieProgressPct <= 1.1f       -> DayNutritionStatus.COMPLETE
            else                             -> DayNutritionStatus.OVER
        }

    companion object {
        fun fromMeals(
            date: String,
            meals: List<Meal>,
            profile: NutritionProfile
        ): DailyNutrition = DailyNutrition(
            date           = date,
            meals          = meals,
            totalCalories  = meals.sumOf { it.estimatedCalories },
            totalProteinG  = meals.sumOf { it.estimatedProteinG.toDouble() }.toFloat(),
            totalCarbsG    = meals.sumOf { it.estimatedCarbsG.toDouble() }.toFloat(),
            totalFatG      = meals.sumOf { it.estimatedFatG.toDouble() }.toFloat(),
            targetCalories = profile.targetCalories,
            targetProteinG = profile.targetProteinG,
            targetCarbsG   = profile.targetCarbsG,
            targetFatG     = profile.targetFatG,
            targetWaterMl  = profile.targetWaterMl
        )
    }
}
