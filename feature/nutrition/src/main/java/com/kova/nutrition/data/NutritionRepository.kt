package com.kova.nutrition.data

import com.kova.nutrition.model.DailyNutrition
import com.kova.nutrition.model.Meal
import com.kova.nutrition.model.NutritionProfile
import java.time.LocalDate

class NutritionRepository {

    private var nutritionProfile: NutritionProfile? = null
    private val meals: MutableList<Meal> = mutableListOf()

    fun saveProfile(profile: NutritionProfile) {
        nutritionProfile = profile
    }

    fun getProfile(): NutritionProfile? = nutritionProfile

    fun saveMeal(meal: Meal) {
        meals.add(meal.copy(id = System.currentTimeMillis()))
    }

    fun getMealsForDate(date: String): List<Meal> =
        meals.filter { it.date == date }

    fun deleteMeal(mealId: Long) {
        meals.removeAll { it.id == mealId }
    }

    fun getDailyNutrition(date: String): DailyNutrition {
        val profile = nutritionProfile ?: return DailyNutrition(date = date)
        return DailyNutrition.fromMeals(date, getMealsForDate(date), profile)
    }

    fun getTodayNutrition(): DailyNutrition =
        getDailyNutrition(LocalDate.now().toString())
}
