package com.kova.nutrition.logic

import com.kova.nutrition.model.DailyNutrition
import com.kova.nutrition.model.DayNutritionStatus
import com.kova.nutrition.model.MealType
import java.time.LocalTime

object NutritionCoach {

    /**
     * Genera el mensaje principal que Kova muestra en la home de nutrición.
     * Combina hora del día + estado calórico + comidas registradas.
     */
    fun getDailyMessage(nutrition: DailyNutrition): String {
        val hour = LocalTime.now().hour

        // Sin datos aún — primer uso del día
        if (nutrition.status == DayNutritionStatus.NOT_STARTED) {
            return getNotStartedMessage(hour)
        }

        return when (nutrition.status) {
            DayNutritionStatus.LOW      -> getLowMessage(hour, nutrition)
            DayNutritionStatus.ON_TRACK -> getOnTrackMessage(hour, nutrition)
            DayNutritionStatus.COMPLETE -> getCompleteMessage(nutrition)
            DayNutritionStatus.OVER     -> getOverMessage(nutrition)
            else                        -> ""
        }
    }

    /**
     * Genera una notificación proactiva según la hora.
     * Devuelve null si no hay nada relevante que decir ahora.
     */
    fun getProactiveNotification(nutrition: DailyNutrition): String? {
        val hour = LocalTime.now().hour
        val hasMealType = { type: MealType ->
            nutrition.meals.any { it.mealType == type }
        }

        return when {
            // Sin desayuno a las 9-10am
            hour in 9..10 && !hasMealType(MealType.BREAKFAST) ->
                "Son las ${hour}:00 y no has desayunado. Tu cuerpo lleva horas sin combustible."

            // Pocas calorías al mediodía
            hour in 13..14 && nutrition.calorieProgressPct < 0.3f ->
                "Llevas solo ${nutrition.totalCalories} kcal. No dejes todo para la noche."

            // Sin comida registrada por la tarde
            hour in 16..17 && nutrition.meals.isEmpty() ->
                "No has registrado nada hoy. ¿Has comido algo?"

            // Resumen nocturno
            hour == 21 && nutrition.status != DayNutritionStatus.NOT_STARTED ->
                getNightSummary(nutrition)

            else -> null
        }
    }

    /**
     * Sugerencia de qué comer basada en calorías y macros restantes.
     */
    fun getMealSuggestion(nutrition: DailyNutrition): String {
        val remaining = nutrition.remainingCalories
        val hour = LocalTime.now().hour

        if (remaining <= 0) return "Ya has alcanzado tu objetivo calórico de hoy."

        val suggestion = when {
            remaining < 200  -> "Algo ligero: una fruta, yogur, o un puñado de frutos secos."
            remaining < 400  -> "Una ensalada con proteína: atún, huevo o pollo a la plancha."
            remaining < 600  -> "Un plato combinado: proteína + verdura + carbohidrato moderado."
            remaining < 900  -> "Puedes permitirte una comida completa. Prioriza proteína."
            else             -> "Tienes margen amplio. No lo desperdicies en ultraprocesados."
        }

        val timing = when (hour) {
            in 6..11  -> "Para el desayuno"
            in 12..15 -> "Para la comida"
            in 16..18 -> "Para la merienda"
            else      -> "Para cenar"
        }

        return "$timing te quedan $remaining kcal. $suggestion"
    }

    // ── Mensajes por estado ───────────────────────────────────────────────────

    private fun getNotStartedMessage(hour: Int): String = when (hour) {
        in 6..9   -> "Buenos días. Registra tu desayuno para empezar el día con Kova."
        in 10..12 -> "Aún no has registrado nada hoy. ¿Has desayunado?"
        in 13..15 -> "Son las $hour:00 y no hay nada registrado. Empieza por la comida."
        else      -> "Todavía estás a tiempo de registrar lo que has comido hoy."
    }

    private fun getLowMessage(hour: Int, nutrition: DailyNutrition): String {
        val remaining = nutrition.remainingCalories
        return when (hour) {
            in 6..12  -> "Buen inicio. Te quedan $remaining kcal para el resto del día."
            in 13..17 -> "Llevas poco. Te quedan $remaining kcal — no te quedes corto."
            else      -> "La noche no es el mejor momento para recuperar calorías. Intenta distribuirlas mejor mañana."
        }
    }

    private fun getOnTrackMessage(hour: Int, nutrition: DailyNutrition): String {
        val pct = (nutrition.calorieProgressPct * 100).toInt()
        return when (hour) {
            in 6..12  -> "Llevas el $pct% de tu objetivo. Buen ritmo para la mañana."
            in 13..17 -> "Vas bien, $pct% completado. Sigue así en la cena."
            else      -> "Casi llegas. ${nutrition.remainingCalories} kcal para cerrar el día perfecto."
        }
    }

    private fun getCompleteMessage(nutrition: DailyNutrition): String {
        val over = nutrition.totalCalories - nutrition.targetCalories
        return if (over <= 50)
            "Objetivo alcanzado. Has gestionado bien tu alimentación hoy."
        else
            "Objetivo cumplido, ${over} kcal por encima. Dentro del margen normal."
    }

    private fun getOverMessage(nutrition: DailyNutrition): String {
        val over = nutrition.totalCalories - nutrition.targetCalories
        return "Te has pasado $over kcal. Mañana puedes compensar reduciendo un poco. Sin obsesión."
    }

    private fun getNightSummary(nutrition: DailyNutrition): String {
        val pct = (nutrition.calorieProgressPct * 100).toInt()
        return when (nutrition.status) {
            DayNutritionStatus.COMPLETE -> "Resumen del día: $pct% del objetivo. Bien hecho."
            DayNutritionStatus.LOW      -> "Resumen: solo el $pct% del objetivo. Intenta distribuir mejor mañana."
            DayNutritionStatus.OVER     -> "Resumen: ${nutrition.totalCalories} kcal, algo por encima. Mañana es otro día."
            else                        -> "Resumen del día: ${nutrition.totalCalories} kcal registradas."
        }
    }
}
