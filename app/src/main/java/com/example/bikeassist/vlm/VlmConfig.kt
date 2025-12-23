package com.example.bikeassist.vlm

import android.content.Context
import com.example.bikeassist.util.AppLogger
import org.json.JSONObject

data class VlmConfig(
    val model: String,
    val maxTokens: Int,
    val temperature: Double,
    val systemPrompt: String,
    val overviewPrompt: String
) {
    companion object {
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        const val DEFAULT_MAX_TOKENS = 320
        const val DEFAULT_TEMPERATURE = 0.2
        const val DEFAULT_SYSTEM_PROMPT =
            "Du bist ein Assistenzsystem fuer sehbehinderte Radfahrer. " +
            "Antworte auf Deutsch in kurzen, strukturierten Saetzen. " +
            "Fokussiere Szene, Hindernisse und eine vorsichtige Handlungsempfehlung. " +
            "Keine Garantien wie 'Weg frei'."
        const val DEFAULT_OVERVIEW_PROMPT =
            "Beschreibe die Szene knapp und klar. " +
            "Nenne Hindernisse und eine sichere, vorsichtige Empfehlung."

        fun defaults(): VlmConfig {
            return VlmConfig(
                model = DEFAULT_MODEL,
                maxTokens = DEFAULT_MAX_TOKENS,
                temperature = DEFAULT_TEMPERATURE,
                systemPrompt = DEFAULT_SYSTEM_PROMPT,
                overviewPrompt = DEFAULT_OVERVIEW_PROMPT
            )
        }
    }
}

object VlmConfigLoader {
    fun load(context: Context): VlmConfig {
        return runCatching {
            val raw = context.assets.open("vlm-config.json").bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val model = root.optString("model", VlmConfig.DEFAULT_MODEL).ifBlank {
                VlmConfig.DEFAULT_MODEL
            }
            val maxTokens = if (root.has("max_tokens")) {
                root.optInt("max_tokens", VlmConfig.DEFAULT_MAX_TOKENS)
            } else {
                VlmConfig.DEFAULT_MAX_TOKENS
            }.takeIf { it > 0 } ?: VlmConfig.DEFAULT_MAX_TOKENS
            val temperature = if (root.has("temperature")) {
                root.optDouble("temperature", VlmConfig.DEFAULT_TEMPERATURE)
            } else {
                VlmConfig.DEFAULT_TEMPERATURE
            }.takeIf { !it.isNaN() } ?: VlmConfig.DEFAULT_TEMPERATURE
            val systemPrompt = root.optString("system_prompt", VlmConfig.DEFAULT_SYSTEM_PROMPT).ifBlank {
                VlmConfig.DEFAULT_SYSTEM_PROMPT
            }
            val overviewPrompt = root.optString("overview_prompt", VlmConfig.DEFAULT_OVERVIEW_PROMPT).ifBlank {
                VlmConfig.DEFAULT_OVERVIEW_PROMPT
            }
            VlmConfig(
                model = model,
                maxTokens = maxTokens,
                temperature = temperature,
                systemPrompt = systemPrompt,
                overviewPrompt = overviewPrompt
            )
        }.getOrElse { ex ->
            AppLogger.e(ex, "Failed to load vlm-config.json, using defaults")
            VlmConfig.defaults()
        }
    }
}
