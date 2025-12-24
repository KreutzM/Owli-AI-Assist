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
        val id = normalizeProfileId(profileId ?: defaultProfileId)
        return profiles.firstOrNull { it.id == id }
            ?: profiles.firstOrNull { it.id == defaultProfileId }
            ?: profiles.first()
    }

    private fun normalizeProfileId(profileId: String): String {
        return when (profileId) {
            "nano_safe" -> "nano-low"
            "nano_fast" -> "nano-high"
            else -> profileId
        }
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
        return listOf(
            VlmProfile(
                id = "gpt4o_default",
                label = "GPT-4o Mini",
                description = "Standardprofil, balanciert",
                model = "openai/gpt-4o-mini",
                maxTokens = VlmConfig.DEFAULT_MAX_TOKENS,
                systemPrompt = safeSystem,
                overviewPrompt = safeOverview,
                temperature = VlmConfig.DEFAULT_TEMPERATURE,
                thinkingEffort = null
            ),
            VlmProfile(
                id = "nano-low",
                label = "Nano Low",
                description = "Reasoning low, sicherheitsbewusst",
                model = "openai/gpt-5-nano",
                maxTokens = 512,
                systemPrompt = safeSystem,
                overviewPrompt = safeOverview,
                temperature = null,
                thinkingEffort = "low"
            ),
            VlmProfile(
                id = "nano-high",
                label = "Nano High",
                description = "Reasoning medium, detailreicher",
                model = "openai/gpt-5-nano",
                maxTokens = 512,
                systemPrompt = safeSystem,
                overviewPrompt = safeOverview,
                temperature = null,
                thinkingEffort = "medium"
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
        val maxTokens = obj.optInt("max_tokens", VlmConfig.DEFAULT_MAX_TOKENS)
            .takeIf { it > 0 } ?: VlmConfig.DEFAULT_MAX_TOKENS
        val temperature = if (obj.has("temperature")) {
            if (obj.isNull("temperature")) {
                null
            } else {
                obj.optDouble("temperature", VlmConfig.DEFAULT_TEMPERATURE).takeIf { !it.isNaN() }
            }
        } else {
            null
        }
        val thinkingEffort = obj.optString("thinking_effort", "").trim().lowercase().ifBlank { null }
            ?.takeIf { it == "low" || it == "medium" || it == "high" }
        return VlmProfile(
            id = id,
            label = label,
            description = description,
            model = model,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            overviewPrompt = overviewPrompt,
            temperature = temperature,
            thinkingEffort = thinkingEffort
        )
    }
}
