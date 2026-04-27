package com.kova.nutrition.logic

import com.kova.nutrition.BuildConfig
import com.kova.nutrition.model.Meal
import com.kova.nutrition.model.InputMethod
import com.kova.nutrition.model.MealType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object FoodEstimator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    /**
     * Recibe una descripción de comida en texto libre y devuelve un Meal
     * con calorías y macros estimados por Claude.
     *
     * Ejemplo: "me comí un menú del día: lentejas y filete con patatas"
     */
    fun estimate(
        description: String,
        mealType: MealType,
        inputMethod: InputMethod = InputMethod.TEXT
    ): Result<Meal> {
        return try {
            val prompt = buildPrompt(description)
            val responseText = callClaudeApi(prompt)
            val meal = parseResponse(responseText, description, mealType, inputMethod)
            Result.success(meal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private fun buildPrompt(description: String): String = """
        Eres un nutricionista experto. El usuario te describe lo que comió.
        Tu tarea es estimar las calorías y macronutrientes de forma realista.
        
        Comida descrita: "$description"
        
        Responde ÚNICAMENTE con un JSON válido con este formato exacto, sin texto adicional:
        {
          "calories": 650,
          "protein_g": 35.0,
          "carbs_g": 72.0,
          "fat_g": 18.0,
          "confidence": "high",
          "note": "Estimación basada en ración estándar española"
        }
        
        Reglas:
        - Usa raciones típicas españolas si no se especifica cantidad
        - confidence puede ser "high", "medium" o "low"
        - note debe ser breve (máximo 60 caracteres)
        - Si la descripción es ambigua, estima por lo alto
        - Solo devuelve el JSON, nada más
    """.trimIndent()

    // ── Llamada a Claude API ──────────────────────────────────────────────────

    private fun callClaudeApi(prompt: String): String {
        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 256)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Claude API error: ${response.code} ${response.body?.string()}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("Respuesta vacía de Claude API")

        // Extraer el texto del content[0].text
        val json = JSONObject(responseBody)
        return json
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }

    // ── Parsear respuesta JSON de Claude ──────────────────────────────────────

    private fun parseResponse(
        responseText: String,
        description: String,
        mealType: MealType,
        inputMethod: InputMethod
    ): Meal {
        // Claude a veces envuelve el JSON en ```json ... ```
        val cleanJson = responseText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleanJson)

        return Meal(
            date               = LocalDate.now().toString(),
            timeHour           = LocalTime.now().hour,
            description        = description,
            inputMethod        = inputMethod,
            mealType           = mealType,
            estimatedCalories  = json.getInt("calories"),
            estimatedProteinG  = json.getDouble("protein_g").toFloat(),
            estimatedCarbsG    = json.getDouble("carbs_g").toFloat(),
            estimatedFatG      = json.getDouble("fat_g").toFloat(),
            confidenceNote     = json.optString("note", "")
        )
    }
}
