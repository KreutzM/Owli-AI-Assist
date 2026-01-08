package com.owlitech.owli.assist.vlm

import android.content.Context
import com.owlitech.owli.assist.util.AppLogger
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
            "nano_fast" -> "nano-fast"
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
        val defaults = VlmProfileDefaults(
            provider = "openrouter",
            family = null,
            systemPrompt = safeSystem,
            overviewPrompt = safeOverview,
            imageSettings = VlmImageSettings(),
            tokenPolicy = VlmTokenPolicy(maxTokens = VlmConfig.DEFAULT_MAX_TOKENS),
            parameterOverrides = VlmParameterOverrides(),
            capabilities = VlmCapabilities(),
            streamingEnabled = false
        )
        return listOf(
            VlmProfile(
                id = "gpt4o_default",
                label = "GPT-4o Mini",
                description = "Standardprofil, balanciert",
                modelId = "openai/gpt-4o-mini",
                provider = defaults.provider,
                family = "gpt4o",
                systemPrompt = defaults.systemPrompt,
                overviewPrompt = defaults.overviewPrompt,
                imageSettings = defaults.imageSettings,
                tokenPolicy = VlmTokenPolicy(maxTokens = VlmConfig.DEFAULT_MAX_TOKENS),
                parameterOverrides = VlmParameterOverrides(temperature = VlmConfig.DEFAULT_TEMPERATURE),
                capabilities = defaults.capabilities,
                streamingEnabled = defaults.streamingEnabled
            ),
            VlmProfile(
                id = "nano-low",
                label = "Nano Low",
                description = "Reasoning low, sicherheitsbewusst",
                modelId = "openai/gpt-5-nano",
                provider = defaults.provider,
                family = "gpt5",
                systemPrompt = defaults.systemPrompt,
                overviewPrompt = defaults.overviewPrompt,
                imageSettings = defaults.imageSettings,
                tokenPolicy = VlmTokenPolicy(
                    maxTokens = 900,
                    reasoningEffort = "low",
                    retry1MaxTokens = 1200,
                    retry2MaxTokens = 1400
                ),
                parameterOverrides = defaults.parameterOverrides,
                capabilities = defaults.capabilities.copy(supportsReasoning = true),
                streamingEnabled = defaults.streamingEnabled
            ),
            VlmProfile(
                id = "nano-fast",
                label = "Nano Fast",
                description = "Sehr kurz, ohne Reasoning",
                modelId = "openai/gpt-5-nano",
                provider = defaults.provider,
                family = "gpt5",
                systemPrompt = defaults.systemPrompt,
                overviewPrompt = defaults.overviewPrompt,
                imageSettings = defaults.imageSettings,
                tokenPolicy = VlmTokenPolicy(
                    maxTokens = 200,
                    reasoningEffort = "minimal",
                    reasoningExclude = true,
                    retry1MaxTokens = 260,
                    retry2MaxTokens = 320
                ),
                parameterOverrides = defaults.parameterOverrides,
                capabilities = defaults.capabilities.copy(supportsReasoning = true),
                streamingEnabled = true
            ),
            VlmProfile(
                id = "nano-high",
                label = "Nano High",
                description = "Reasoning medium, detailreicher",
                modelId = "openai/gpt-5-nano",
                provider = defaults.provider,
                family = "gpt5",
                systemPrompt = defaults.systemPrompt,
                overviewPrompt = defaults.overviewPrompt,
                imageSettings = defaults.imageSettings,
                tokenPolicy = VlmTokenPolicy(
                    maxTokens = 900,
                    reasoningEffort = "medium",
                    retry1MaxTokens = 1200,
                    retry2MaxTokens = 1400
                ),
                parameterOverrides = defaults.parameterOverrides,
                capabilities = defaults.capabilities.copy(supportsReasoning = true),
                streamingEnabled = defaults.streamingEnabled
            )
        )
    }

    private fun parse(raw: String): VlmProfilesConfig {
        val root = JSONObject(raw)
        val defaults = parseDefaults(root)
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = buildList {
            for (i in 0 until profilesArray.length()) {
                val obj = profilesArray.optJSONObject(i) ?: continue
                val profile = parseProfile(obj, defaults) ?: continue
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

    private fun parseDefaults(root: JSONObject): VlmProfileDefaults {
        val obj = root.optJSONObject("defaults")
        val provider = obj?.optString("provider")?.trim().orEmpty().ifBlank { "openrouter" }
        val family = obj?.optString("family")?.trim()?.takeIf { it.isNotBlank() }
        val systemPrompt = obj?.optString("system_prompt")?.trim()?.takeIf { it.isNotBlank() }
            ?: VlmConfig.DEFAULT_SYSTEM_PROMPT
        val overviewPrompt = obj?.optString("overview_prompt")?.trim()?.takeIf { it.isNotBlank() }
            ?: VlmConfig.DEFAULT_OVERVIEW_PROMPT
        val imageSettings = parseImageSettings(obj?.optJSONObject("image"), VlmImageSettings())
        val tokenPolicy = parseTokenPolicy(obj?.optJSONObject("token_policy"), VlmTokenPolicy(VlmConfig.DEFAULT_MAX_TOKENS), null)
        val parameterOverrides = parseParameterOverrides(obj?.optJSONObject("parameter_overrides"), VlmParameterOverrides(), null)
        val capabilities = parseCapabilities(obj?.optJSONObject("capabilities"), VlmCapabilities())
        val streamingEnabled = obj?.optBoolean("streaming_enabled", false) ?: false
        return VlmProfileDefaults(
            provider = provider,
            family = family,
            systemPrompt = systemPrompt,
            overviewPrompt = overviewPrompt,
            imageSettings = imageSettings,
            tokenPolicy = tokenPolicy,
            parameterOverrides = parameterOverrides,
            capabilities = capabilities,
            streamingEnabled = streamingEnabled
        )
    }

    private fun parseProfile(obj: JSONObject, defaults: VlmProfileDefaults): VlmProfile? {
        val id = obj.optString("id", "").trim()
        val label = obj.optString("label", "").trim()
        val modelId = obj.optString("model_id", "").trim()
            .ifBlank { obj.optString("model", "").trim() }
        val systemPrompt = obj.optString("system_prompt", "").trim().ifBlank { defaults.systemPrompt }
        if (id.isBlank() || label.isBlank() || modelId.isBlank() || systemPrompt.isBlank()) {
            AppLogger.w(
                "VLM",
                "VLM profile invalid: id=$id label=$label modelId=$modelId systemPrompt=${systemPrompt.isNotBlank()}"
            )
            return null
        }
        val description = obj.optString("description", "").trim().ifBlank { null }
        val overviewPrompt = obj.optString("overview_prompt", "").trim().ifBlank {
            defaults.overviewPrompt
        }
        val provider = obj.optString("provider", defaults.provider).trim().ifBlank { defaults.provider }
        val family = obj.optString("family", "").trim().ifBlank { defaults.family }
        val imageSettings = parseImageSettings(obj.optJSONObject("image"), defaults.imageSettings)
        val tokenPolicy = parseTokenPolicy(obj.optJSONObject("token_policy"), defaults.tokenPolicy, obj)
        val parameterOverrides = parseParameterOverrides(obj.optJSONObject("parameter_overrides"), defaults.parameterOverrides, obj)
        val capabilities = parseCapabilities(obj.optJSONObject("capabilities"), defaults.capabilities)
        val autoScan = parseAutoScan(obj.optJSONObject("auto_scan"))
        val streamingEnabled = if (obj.has("streaming_enabled")) {
            obj.optBoolean("streaming_enabled", defaults.streamingEnabled)
        } else {
            defaults.streamingEnabled
        }
        return VlmProfile(
            id = id,
            label = label,
            description = description,
            modelId = modelId,
            provider = provider,
            family = family,
            systemPrompt = systemPrompt,
            overviewPrompt = overviewPrompt,
            imageSettings = imageSettings,
            tokenPolicy = tokenPolicy,
            parameterOverrides = parameterOverrides,
            capabilities = capabilities,
            streamingEnabled = streamingEnabled,
            autoScan = autoScan
        )
    }

    private fun parseImageSettings(obj: JSONObject?, defaults: VlmImageSettings): VlmImageSettings {
        if (obj == null) return defaults
        val maxSidePx = obj.optInt("max_side_px", defaults.maxSidePx).takeIf { it > 0 } ?: defaults.maxSidePx
        val jpegQuality = obj.optInt("jpeg_quality", defaults.jpegQuality).takeIf { it in 10..100 } ?: defaults.jpegQuality
        val detail = obj.optString("detail", "").trim().ifBlank { defaults.detail }
        return VlmImageSettings(
            maxSidePx = maxSidePx,
            jpegQuality = jpegQuality,
            detail = detail
        )
    }

    private fun parseTokenPolicy(
        obj: JSONObject?,
        defaults: VlmTokenPolicy,
        legacy: JSONObject?
    ): VlmTokenPolicy {
        val legacyMax = legacy?.optInt("max_tokens", -1)?.takeIf { it > 0 }
        val maxTokens = obj?.optInt("max_tokens", defaults.maxTokens)?.takeIf { it > 0 }
            ?: legacyMax
            ?: defaults.maxTokens
        val rawEffort = obj?.optString("reasoning_effort", "")?.trim()?.takeIf { it.isNotBlank() }
            ?: legacy?.optString("thinking_effort", "")?.trim()?.takeIf { it.isNotBlank() }
        val reasoningEffort = VlmModelFamilyPolicy.sanitizeReasoningEffort(rawEffort)
            ?: defaults.reasoningEffort
        val retry1 = obj?.optInt("retry1_max_tokens", -1)?.takeIf { it > 0 }
        val retry2 = obj?.optInt("retry2_max_tokens", -1)?.takeIf { it > 0 }
        val reasoningExclude = obj?.optBoolean("reasoning_exclude", defaults.reasoningExclude) ?: defaults.reasoningExclude
        return VlmTokenPolicy(
            maxTokens = maxTokens,
            reasoningEffort = reasoningEffort,
            reasoningExclude = reasoningExclude,
            retry1MaxTokens = retry1 ?: defaults.retry1MaxTokens,
            retry2MaxTokens = retry2 ?: defaults.retry2MaxTokens
        )
    }

    private fun parseParameterOverrides(
        obj: JSONObject?,
        defaults: VlmParameterOverrides,
        legacy: JSONObject?
    ): VlmParameterOverrides {
        val temp = when {
            obj != null && obj.has("temperature") -> {
                if (obj.isNull("temperature")) null else obj.optDouble("temperature", Double.NaN).takeIf { !it.isNaN() }
            }
            legacy != null && legacy.has("temperature") -> {
                if (legacy.isNull("temperature")) null else legacy.optDouble("temperature", Double.NaN).takeIf { !it.isNaN() }
            }
            else -> defaults.temperature
        }
        val effort = when {
            obj != null && obj.has("reasoning_effort") -> obj.optString("reasoning_effort", "").trim()
            else -> defaults.reasoningEffort
        }
        return VlmParameterOverrides(
            temperature = temp,
            reasoningEffort = VlmModelFamilyPolicy.sanitizeReasoningEffort(effort)
        )
    }

    private fun parseCapabilities(obj: JSONObject?, defaults: VlmCapabilities): VlmCapabilities {
        if (obj == null) return defaults
        val supportsVision = obj.optBoolean("supports_vision", defaults.supportsVision)
        val supportsReasoning = obj.optBoolean("supports_reasoning", defaults.supportsReasoning)
        val supportsJson = obj.optBoolean("supports_json", defaults.supportsJson)
        return VlmCapabilities(
            supportsVision = supportsVision,
            supportsReasoning = supportsReasoning,
            supportsJson = supportsJson
        )
    }

    private fun parseAutoScan(obj: JSONObject?): VlmAutoScanConfig? {
        if (obj == null) return null
        val enabledByDefault = obj.optBoolean("enabled_by_default", false)
        val intervalMs = obj.optLong("interval_ms", 2000L)
        val speakFreeEveryMs = obj.optLong("speak_free_every_ms", 10000L)
        return VlmAutoScanConfig(
            enabledByDefault = enabledByDefault,
            intervalMs = intervalMs,
            speakFreeEveryMs = speakFreeEveryMs
        )
    }

    private data class VlmProfileDefaults(
        val provider: String,
        val family: String?,
        val systemPrompt: String,
        val overviewPrompt: String,
        val imageSettings: VlmImageSettings,
        val tokenPolicy: VlmTokenPolicy,
        val parameterOverrides: VlmParameterOverrides,
        val capabilities: VlmCapabilities,
        val streamingEnabled: Boolean
    )
}
