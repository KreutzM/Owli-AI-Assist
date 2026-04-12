package com.owlitech.owli.assist.vlm

import android.content.Context
import com.owlitech.owli.assist.settings.VlmTransportMode
import com.owlitech.owli.assist.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject

enum class VlmProfilesSource {
    REMOTE_BACKEND,
    CACHED_BACKEND,
    LOCAL_REGISTRY_ASSET,
    LOCAL_LEGACY_ASSET,
    HARDCODED_FALLBACK
}

data class VlmProfilesConfig(
    val profiles: List<VlmProfile>,
    val defaultProfileId: String,
    val source: VlmProfilesSource
) {
    fun profilesForTransport(mode: VlmTransportMode): List<VlmProfile> {
        val filtered = profiles.filter { it.isAvailableIn(mode) }
        return if (filtered.isNotEmpty()) filtered else profiles
    }

    fun resolve(profileId: String?, mode: VlmTransportMode? = null): VlmProfile {
        val candidates = mode?.let(::profilesForTransport).orEmpty().ifEmpty {
            if (profiles.isNotEmpty()) profiles else VlmProfileLoader.fallbackProfiles()
        }
        val normalizedId = normalizeProfileId(profileId ?: defaultProfileId)
        val normalizedDefaultId = normalizeProfileId(defaultProfileId)
        return candidates.firstOrNull { it.id == normalizedId }
            ?: candidates.firstOrNull { it.id == normalizedDefaultId }
            ?: candidates.first()
    }

    private fun normalizeProfileId(profileId: String): String {
        return when (profileId) {
            "nano_safe" -> "nano-low"
            "nano_fast" -> "nano-fast"
            else -> profileId
        }
    }
}

internal data class PublicVlmProfileRegistry(
    val defaultProfileId: String,
    val profiles: List<PublicVlmProfile>
)

internal data class LocalRegistryConfig(
    val defaultProfileId: String,
    val profiles: List<LocalRegistryProfile>
) {
    fun toProfilesConfig(source: VlmProfilesSource = VlmProfilesSource.LOCAL_REGISTRY_ASSET): VlmProfilesConfig {
        val effectiveProfiles = profiles.map { it.toEffectiveProfile() }
        val resolvedDefault = effectiveProfiles.firstOrNull { it.id == defaultProfileId }?.id
            ?: effectiveProfiles.firstOrNull()?.id
            ?: VlmProfileLoader.fallbackConfig().defaultProfileId
        return VlmProfilesConfig(
            profiles = effectiveProfiles.ifEmpty { VlmProfileLoader.fallbackProfiles() },
            defaultProfileId = resolvedDefault,
            source = source
        )
    }
}

internal data class LocalRegistryProfile(
    val id: String,
    val label: String,
    val description: String?,
    val backendAvailable: Boolean,
    val backendStreamingEnabled: Boolean,
    val embeddedDebugAllowed: Boolean,
    val byok: ByokProfileConfig?
) {
    fun toEffectiveProfile(publicProfile: PublicVlmProfile? = null): VlmProfile {
        val label = publicProfile?.label ?: label
        val description = publicProfile?.description ?: description
        val backendAvailable = publicProfile?.backendAvailable ?: backendAvailable
        val directByokAvailable = publicProfile?.directByokAvailable ?: (byok != null)
        val streamingEnabled = when {
            directByokAvailable && byok != null -> byok.streamingEnabled
            publicProfile != null && publicProfile.backendAvailable -> publicProfile.backendSupportsStreaming
            else -> backendStreamingEnabled
        }
        return VlmProfile(
            id = id,
            label = label,
            description = description,
            modelId = byok?.modelId ?: "owli-backend/$id",
            provider = byok?.provider ?: if (backendAvailable) "owli-backend" else "openrouter",
            family = byok?.family,
            systemPrompt = byok?.systemPrompt ?: VlmConfig.DEFAULT_SYSTEM_PROMPT,
            overviewPrompt = byok?.overviewPrompt ?: VlmConfig.DEFAULT_OVERVIEW_PROMPT,
            imageSettings = byok?.imageSettings ?: VlmImageSettings(),
            tokenPolicy = byok?.tokenPolicy ?: VlmTokenPolicy(VlmConfig.DEFAULT_MAX_TOKENS),
            parameterOverrides = byok?.parameterOverrides ?: VlmParameterOverrides(),
            capabilities = byok?.capabilities ?: VlmCapabilities(),
            streamingEnabled = streamingEnabled,
            autoScan = byok?.autoScan,
            backendManagedAvailable = backendAvailable,
            directByokAvailable = directByokAvailable,
            embeddedDebugAvailable = directByokAvailable && embeddedDebugAllowed
        )
    }
}

internal data class ByokProfileConfig(
    val provider: String,
    val modelId: String,
    val family: String?,
    val systemPrompt: String,
    val overviewPrompt: String,
    val imageSettings: VlmImageSettings,
    val tokenPolicy: VlmTokenPolicy,
    val parameterOverrides: VlmParameterOverrides,
    val capabilities: VlmCapabilities,
    val streamingEnabled: Boolean,
    val autoScan: VlmAutoScanConfig?
)

internal data class PublicVlmProfile(
    val id: String,
    val label: String,
    val description: String?,
    val backendAvailable: Boolean,
    val backendSupportsStreaming: Boolean,
    val directByokAvailable: Boolean,
    val directByokSupportsStreaming: Boolean
)

object VlmProfileLoader {

    fun loadLegacyAsset(context: Context): VlmProfilesConfig {
        return runCatching {
            val raw = context.assets.open("vlm-profiles.json").bufferedReader().use { it.readText() }
            parseLegacy(raw)
        }.getOrElse { ex ->
            AppLogger.e(ex, "Failed to load vlm-profiles.json, using fallback profiles")
            fallbackConfig()
        }
    }

    fun loadLocalRegistryAsset(context: Context): VlmProfilesConfig? {
        return runCatching {
            val raw = context.assets.open("vlm-profile-registry.json").bufferedReader().use { it.readText() }
            parseLocalRegistry(raw)?.toProfilesConfig()
        }.getOrElse { ex ->
            AppLogger.e(ex, "Failed to load vlm-profile-registry.json")
            null
        }
    }

    internal fun parsePublicRegistry(raw: String): PublicVlmProfileRegistry? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val schemaVersion = root.optString("schemaVersion").trim()
        if (schemaVersion != "vlm_profile_registry/v1") {
            return null
        }
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = buildList {
            for (i in 0 until profilesArray.length()) {
                val obj = profilesArray.optJSONObject(i) ?: continue
                parsePublicProfile(obj)?.let(::add)
            }
        }
        if (profiles.isEmpty()) return null
        val defaultId = root.optString("defaultProfileId", profiles.first().id).trim().ifBlank {
            profiles.first().id
        }
        val resolvedDefault = profiles.firstOrNull { it.id == defaultId }?.id ?: profiles.first().id
        return PublicVlmProfileRegistry(
            defaultProfileId = resolvedDefault,
            profiles = profiles
        )
    }

    internal fun mergePublicRegistry(
        publicRegistry: PublicVlmProfileRegistry,
        localRegistry: LocalRegistryConfig?
    ): VlmProfilesConfig {
        val localProfilesById = localRegistry?.profiles?.associateBy { it.id }.orEmpty()
        val mergedProfiles = publicRegistry.profiles.mapNotNull { publicProfile ->
            val localProfile = localProfilesById[publicProfile.id]
            mergeProfile(publicProfile, localProfile)
        }
        if (mergedProfiles.isEmpty()) {
            return localRegistry?.toProfilesConfig() ?: fallbackConfig()
        }
        val defaultId = mergedProfiles.firstOrNull { it.id == publicRegistry.defaultProfileId }?.id
            ?: mergedProfiles.first().id
        return VlmProfilesConfig(
            profiles = mergedProfiles,
            defaultProfileId = defaultId,
            source = VlmProfilesSource.REMOTE_BACKEND
        )
    }

    fun fallbackConfig(): VlmProfilesConfig {
        val profiles = fallbackProfiles()
        return VlmProfilesConfig(
            profiles = profiles,
            defaultProfileId = profiles.first().id,
            source = VlmProfilesSource.HARDCODED_FALLBACK
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

    internal fun parseLegacy(raw: String): VlmProfilesConfig {
        val root = JSONObject(raw)
        val defaults = parseLegacyDefaults(root)
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = buildList {
            for (i in 0 until profilesArray.length()) {
                val obj = profilesArray.optJSONObject(i) ?: continue
                val profile = parseLegacyProfile(obj, defaults) ?: continue
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
        return VlmProfilesConfig(
            profiles = profiles,
            defaultProfileId = resolvedDefault,
            source = VlmProfilesSource.LOCAL_LEGACY_ASSET
        )
    }

    internal fun parseRegistryAsset(raw: String): VlmProfilesConfig {
        return parseLocalRegistry(raw)?.toProfilesConfig() ?: fallbackConfig()
    }

    internal fun parseLocalRegistry(raw: String): LocalRegistryConfig? {
        val root = JSONObject(raw)
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = buildList {
            for (i in 0 until profilesArray.length()) {
                val obj = profilesArray.optJSONObject(i) ?: continue
                val profile = parseRegistryProfile(obj) ?: continue
                add(profile)
            }
        }
        if (profiles.isEmpty()) return null
        val defaultId = root.optString("default_profile_id", profiles.first().id).ifBlank {
            profiles.first().id
        }
        val resolvedDefault = profiles.firstOrNull { it.id == defaultId }?.id ?: profiles.first().id
        return LocalRegistryConfig(
            profiles = profiles,
            defaultProfileId = resolvedDefault
        )
    }

    private fun parseLegacyDefaults(root: JSONObject): VlmProfileDefaults {
        val obj = root.optJSONObject("defaults")
        val provider = obj?.optString("provider")?.trim().orEmpty().ifBlank { "openrouter" }
        val family = obj?.optString("family")?.trim()?.takeIf { it.isNotBlank() }
        val systemPrompt = obj?.optString("system_prompt")?.trim()?.takeIf { it.isNotBlank() }
            ?: VlmConfig.DEFAULT_SYSTEM_PROMPT
        val overviewPrompt = obj?.optString("overview_prompt")?.trim()?.takeIf { it.isNotBlank() }
            ?: VlmConfig.DEFAULT_OVERVIEW_PROMPT
        val imageSettings = parseImageSettings(obj?.optJSONObject("image"), VlmImageSettings())
        val tokenPolicy = parseTokenPolicy(
            obj?.optJSONObject("token_policy"),
            VlmTokenPolicy(VlmConfig.DEFAULT_MAX_TOKENS),
            null
        )
        val parameterOverrides = parseParameterOverrides(
            obj?.optJSONObject("parameter_overrides"),
            VlmParameterOverrides(),
            null
        )
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

    private fun parseLegacyProfile(obj: JSONObject, defaults: VlmProfileDefaults): VlmProfile? {
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
        val parameterOverrides = parseParameterOverrides(
            obj.optJSONObject("parameter_overrides"),
            defaults.parameterOverrides,
            obj
        )
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

    private fun parseRegistryProfile(obj: JSONObject): LocalRegistryProfile? {
        val id = obj.optString("id").trim()
        val label = obj.optString("label").trim()
        if (id.isBlank() || label.isBlank()) {
            return null
        }
        val description = obj.optString("description").trim().ifBlank { null }
        val availability = obj.optString("availability").trim().lowercase().ifBlank { "both" }
        val backendAvailable = availability == "backend" || availability == "both"
        val directByokAvailable = availability == "byok" || availability == "both"
        val embeddedDebugAllowed = obj.optJSONObject("debug")
            ?.optBoolean("embedded_key_allowed", directByokAvailable)
            ?: directByokAvailable
        val backendBlock = obj.optJSONObject("backend")
        val byokBlock = obj.optJSONObject("byok")
        val byok = parseByokProfileConfig(byokBlock)
        return LocalRegistryProfile(
            id = id,
            label = label,
            description = description,
            backendAvailable = backendAvailable,
            backendStreamingEnabled = backendBlock?.optBoolean("supports_streaming", false) ?: false,
            embeddedDebugAllowed = embeddedDebugAllowed,
            byok = if (directByokAvailable) byok else null
        )
    }

    private fun parseByokProfileConfig(byokBlock: JSONObject?): ByokProfileConfig? {
        if (byokBlock == null) return null
        val provider = byokBlock.optString("provider").trim().ifBlank { "openrouter" }
        val modelId = byokBlock.optString("model_id").trim()
        val systemPrompt = byokBlock.optString("system_prompt").trim()
        if (modelId.isBlank() || systemPrompt.isBlank()) return null
        val overviewPrompt = byokBlock.optString("overview_prompt").trim().ifBlank {
            VlmConfig.DEFAULT_OVERVIEW_PROMPT
        }
        return ByokProfileConfig(
            provider = provider,
            modelId = modelId,
            family = byokBlock.optString("family").trim().ifBlank { null },
            systemPrompt = systemPrompt,
            overviewPrompt = overviewPrompt,
            imageSettings = parseImageSettings(byokBlock.optJSONObject("image"), VlmImageSettings()),
            tokenPolicy = parseTokenPolicy(
                byokBlock.optJSONObject("token_policy"),
                VlmTokenPolicy(VlmConfig.DEFAULT_MAX_TOKENS),
                null
            ),
            parameterOverrides = parseParameterOverrides(
                byokBlock.optJSONObject("parameter_overrides"),
                VlmParameterOverrides(),
                null
            ),
            capabilities = parseCapabilities(byokBlock.optJSONObject("capabilities"), VlmCapabilities()),
            streamingEnabled = byokBlock.optBoolean("streaming_enabled", false),
            autoScan = parseAutoScan(byokBlock.optJSONObject("auto_scan"))
        )
    }

    private fun parsePublicProfile(obj: JSONObject): PublicVlmProfile? {
        val id = obj.optString("id").trim()
        val label = obj.optString("label").trim()
        if (id.isBlank() || label.isBlank()) {
            return null
        }
        val transports = obj.optJSONObject("transports")
        val backend = transports?.optJSONObject("backend")
        val byok = transports?.optJSONObject("byok")
        return PublicVlmProfile(
            id = id,
            label = label,
            description = obj.optString("description").trim().ifBlank { null },
            backendAvailable = backend?.optBoolean("available", false) ?: false,
            backendSupportsStreaming = backend?.optBoolean("supportsStreaming", false) ?: false,
            directByokAvailable = byok?.optBoolean("available", false) ?: false,
            directByokSupportsStreaming = byok?.optBoolean("supportsStreaming", false) ?: false
        )
    }

    private fun mergeProfile(publicProfile: PublicVlmProfile, localProfile: LocalRegistryProfile?): VlmProfile {
        if (localProfile != null) {
            return localProfile.toEffectiveProfile(publicProfile)
        }
        val defaultTokenPolicy = VlmTokenPolicy(maxTokens = VlmConfig.DEFAULT_MAX_TOKENS)
        return VlmProfile(
            id = publicProfile.id,
            label = publicProfile.label,
            description = publicProfile.description,
            modelId = "owli-backend/${publicProfile.id}",
            provider = if (publicProfile.backendAvailable) "owli-backend" else "openrouter",
            family = null,
            systemPrompt = VlmConfig.DEFAULT_SYSTEM_PROMPT,
            overviewPrompt = VlmConfig.DEFAULT_OVERVIEW_PROMPT,
            imageSettings = VlmImageSettings(),
            tokenPolicy = defaultTokenPolicy,
            parameterOverrides = VlmParameterOverrides(),
            capabilities = VlmCapabilities(),
            streamingEnabled = if (publicProfile.backendAvailable) {
                publicProfile.backendSupportsStreaming
            } else {
                publicProfile.directByokSupportsStreaming
            },
            backendManagedAvailable = publicProfile.backendAvailable,
            directByokAvailable = publicProfile.directByokAvailable,
            embeddedDebugAvailable = publicProfile.directByokAvailable
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
