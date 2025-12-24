package com.example.bikeassist.vlm

import android.content.Context
import com.example.bikeassist.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject

data class VlmProfilesConfig(
    val profiles: List<VlmProfile>,
    val defaultProfileId: String
) {
    fun resolve(profileId: String?): VlmProfile {
        if (profiles.isEmpty()) {
            return VlmProfileLoader.fallbackProfiles().first()
        }
        val id = profileId ?: defaultProfileId
        return profiles.firstOrNull { it.id == id }
            ?: profiles.firstOrNull { it.id == defaultProfileId }
            ?: profiles.first()
    }
}

object VlmProfileLoader {
    fun load(context: Context): VlmProfilesConfig {
        return runCatching {
            val raw = context.assets.open("vlm-profiles.json").bufferedReader().use { it.readText() }
            parse(raw)
        }.getOrElse { ex ->
            AppLogger.e(ex, "Failed to load vlm-profiles.json, using fallback profiles")
            fallbackConfig()
        }
    }

    fun fallbackConfig(): VlmProfilesConfig {
        val profiles = fallbackProfiles()
        return VlmProfilesConfig(
            profiles = profiles,
            defaultProfileId = profiles.first().id
        )
    }

    fun fallbackProfiles(): List<VlmProfile> {
        val safeSystem = VlmConfig.DEFAULT_SYSTEM_PROMPT
        val safeOverview = VlmConfig.DEFAULT_OVERVIEW_PROMPT
        val fastSystem =
            "Du bist ein Assistenzsystem fuer sehbehinderte Radfahrer. " +
                "Antworte sehr kurz und klar auf Deutsch. " +
                "Fokus: Hindernisse und vorsichtige Empfehlung. " +
                "Keine Garantien wie 'Weg frei'."
        val fastOverview = "Sehr kurz: Hindernisse + vorsichtige Empfehlung."
        return listOf(
            VlmProfile(
                id = "nano_safe",
                label = "Nano Safe",
                description = "Sicher & konservativ, klare Hinweise",
                model = "openai/gpt-5-nano",
                temperature = 0.15,
                maxTokens = 360,
                systemPrompt = safeSystem,
                overviewPrompt = safeOverview,
                thinkingEnabled = false,
                thinkingBudgetTokens = null
            ),
            VlmProfile(
                id = "nano_fast",
                label = "Nano Fast",
                description = "Schneller, knapper, weniger Tokens",
                model = "openai/gpt-5-nano",
                temperature = 0.35,
                maxTokens = 200,
                systemPrompt = fastSystem,
                overviewPrompt = fastOverview,
                thinkingEnabled = false,
                thinkingBudgetTokens = null
            )
        )
    }

    private fun parse(raw: String): VlmProfilesConfig {
        val root = JSONObject(raw)
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = buildList {
            for (i in 0 until profilesArray.length()) {
                val obj = profilesArray.optJSONObject(i) ?: continue
                val profile = parseProfile(obj) ?: continue
                add(profile)
            }
        }
        if (profiles.isEmpty()) {
            return fallbackConfig()
        }
        val defaultId = root.optString("default_profile_id", profiles.first().id).ifBlank {
            profiles.first().id
        }
        val resolvedDefault = profiles.firstOrNull { it.id == defaultId }?.id ?: profiles.first().id
        return VlmProfilesConfig(profiles = profiles, defaultProfileId = resolvedDefault)
    }

    private fun parseProfile(obj: JSONObject): VlmProfile? {
        val id = obj.optString("id", "").trim()
        val label = obj.optString("label", "").trim()
        val model = obj.optString("model", "").trim()
        val systemPrompt = obj.optString("system_prompt", "").trim()
        if (id.isBlank() || label.isBlank() || model.isBlank() || systemPrompt.isBlank()) {
            return null
        }
        val description = obj.optString("description", "").trim().ifBlank { null }
        val overviewPrompt = obj.optString("overview_prompt", "").trim().ifBlank {
            VlmConfig.DEFAULT_OVERVIEW_PROMPT
        }
        val temperature = obj.optDouble("temperature", VlmConfig.DEFAULT_TEMPERATURE)
        val maxTokens = obj.optInt("max_tokens", VlmConfig.DEFAULT_MAX_TOKENS)
        val thinkingObj = obj.optJSONObject("thinking")
        val thinkingEnabled = thinkingObj?.optBoolean("enabled", false) ?: false
        val thinkingBudget = if (thinkingObj?.has("budget_tokens") == true) {
            thinkingObj.optInt("budget_tokens").takeIf { it > 0 }
        } else {
            null
        }
        return VlmProfile(
            id = id,
            label = label,
            description = description,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            overviewPrompt = overviewPrompt,
            thinkingEnabled = thinkingEnabled,
            thinkingBudgetTokens = thinkingBudget
        )
    }
}
