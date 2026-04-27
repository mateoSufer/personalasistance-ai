package com.kova.nutrition.logic

import com.kova.nutrition.model.ActivityLevel
import com.kova.nutrition.model.NutritionProfile
import com.kova.nutrition.model.PhysicalGoal
import kotlin.math.roundToInt

object CalorieCalculator {

    private val ACTIVITY_MULTIPLIERS = mapOf(
        ActivityLevel.SEDENTARY         to 1.2f,
        ActivityLevel.LIGHTLY_ACTIVE    to 1.375f,
        ActivityLevel.MODERATELY_ACTIVE to 1.55f,
        ActivityLevel.VERY_ACTIVE       to 1.725f,
        ActivityLevel.EXTRA_ACTIVE      to 1.9f
    )

    private val GOAL_CALORIE_DELTA = mapOf(
        PhysicalGoal.LOSE_WEIGHT     to -400,
        PhysicalGoal.MAINTAIN_WEIGHT to 0,
        PhysicalGoal.GAIN_MUSCLE     to +300
    )

    fun calculate(profile: NutritionProfile): NutritionProfile {
        val tdee = calculateTDEE(profile)
        val targetCalories = applyGoalAdjustment(tdee, profile.goal)
        val (proteinG, carbsG, fatG) = calculateMacros(targetCalories, profile)
        val water = calculateWater(profile)

        return profile.copy(
            targetCalories = targetCalories,
            targetProteinG = proteinG,
            targetCarbsG   = carbsG,
            targetFatG     = fatG,
            targetWaterMl  = water
        )
    }

    fun calculateTDEE(profile: NutritionProfile): Int {
        val bmr = calculateBMR(profile)
        val multiplier = ACTIVITY_MULTIPLIERS[profile.activityLevel] ?: 1.2f
        return (bmr * multiplier).roundToInt()
    }

    private fun calculateBMR(profile: NutritionProfile): Float {
        val base = (10f * profile.weightKg) +
                   (6.25f * profile.heightCm) -
                   (5f * profile.ageYears)
        return if (profile.isMale) base + 5f else base - 161f
    }

    private fun applyGoalAdjustment(tdee: Int, goal: PhysicalGoal): Int {
        val delta = GOAL_CALORIE_DELTA[goal] ?: 0
        return (tdee + delta).coerceAtLeast(1200)
    }

    private fun calculateMacros(calories: Int, profile: NutritionProfile): Triple<Int, Int, Int> {
        val (proteinPct, carbsPct, fatPct) = when (profile.goal) {
            PhysicalGoal.LOSE_WEIGHT     -> Triple(0.35f, 0.35f, 0.30f)
            PhysicalGoal.MAINTAIN_WEIGHT -> Triple(0.30f, 0.40f, 0.30f)
            PhysicalGoal.GAIN_MUSCLE     -> Triple(0.35f, 0.45f, 0.20f)
        }
        return Triple(
            ((calories * proteinPct) / 4f).roundToInt(),
            ((calories * carbsPct)   / 4f).roundToInt(),
            ((calories * fatPct)     / 9f).roundToInt()
        )
    }

    private fun calculateWater(profile: NutritionProfile): Int {
        val base = (35 * profile.weightKg).roundToInt()
        val bonus = if (profile.activityLevel in listOf(
                ActivityLevel.VERY_ACTIVE,
                ActivityLevel.EXTRA_ACTIVE
            )) 500 else 0
        return base + bonus
    }
}
