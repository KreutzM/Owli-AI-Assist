package com.owlitech.owli.assist.vlm

import com.owlitech.owli.assist.util.AppLogger
import com.owlitech.owli.assist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val VLM_LOG_TAG = "VLM"
private const val FINAL_ONLY_INSTRUCTION =
    "Gib NUR die finale Antwort im content aus. Keine Zwischenschritte."
private const val MISSING_OPENROUTER_CLIENT_KEY_MESSAGE = "OpenRouter client key fehlt."

/**
 * Uses the current interim OpenRouter setup where the provider client key comes from BuildConfig.
 * That keeps shipped builds functional today, but it is app-shipped material, not secure storage.
 */
class OpenRouterVlmClient(
    private var profile: VlmProfile,
    apiKey: String = BuildConfig.OPENROUTER_API_KEY,
    endpoint: String = "https://openrouter.ai/api/v1/chat/completions",
    private val provider: VlmProvider = OpenRouterProvider(apiKey, endpoint)
) : VlmClient {

    override val isConfigured: Boolean = apiKey.isNotBlank()

    override suspend fun chat(
        messages: List<VlmChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        return runChat(messages, maxTokens, temperature, stream = false, callback = null)
    }

    override suspend fun chatStreaming(
        messages: List<VlmChatMessage>,
        callback: VlmStreamingCallback,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        return runChat(messages, maxTokens, temperature, stream = true, callback = callback)
    }

    private suspend fun runChat(
        messages: List<VlmChatMessage>,
        maxTokens: Int,
        temperature: Double,
        stream: Boolean,
        callback: VlmStreamingCallback?
    ): VlmClientResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw IllegalStateException(MISSING_OPENROUTER_CLIENT_KEY_MESSAGE)
        }
        val family = VlmModelFamilyPolicy.resolveFamily(profile)
        val effectiveTokenPolicy = resolveTokenPolicy(profile, maxTokens, family)
        val resolvedTemperature = resolveTemperature(profile, temperature)
        val allowReasoning = VlmModelFamilyPolicy.allowReasoning(family) || profile.capabilities.supportsReasoning
        val baseOptions = VlmRequestOptions(
            maxTokens = effectiveTokenPolicy.maxTokens,
            temperature = resolvedTemperature,
            reasoningEffort = VlmModelFamilyPolicy.sanitizeReasoningEffort(
                profile.parameterOverrides.reasoningEffort ?: effectiveTokenPolicy.reasoningEffort
            ),
            reasoningExclude = effectiveTokenPolicy.reasoningExclude,
            includeReasoning = allowReasoning,
            stream = stream
        )
        val retryPlans = VlmModelFamilyPolicy.buildRetryPlans(family, baseOptions, effectiveTokenPolicy)
        var attempt = 0
        var lastResult: VlmProviderResult? = null
        var lastPlanLabel: String? = null

        for (plan in retryPlans) {
            lastPlanLabel = plan.label
            val attemptMessages = if (plan.addFinalOnlyInstruction) {
                addFinalOnlyInstruction(messages)
            } else {
                messages
            }
            val tempText = plan.options.temperature?.toString() ?: "unset"
            val reasoningText = plan.options.reasoningEffort ?: "unset"
            val excludeText = if (plan.options.reasoningExclude) "true" else "false"
            AppLogger.d(
                VLM_LOG_TAG,
                "OpenRouter request: model=${profile.modelId} profile=${profile.id} attempt=${plan.label} " +
                    "max_tokens=${plan.options.maxTokens} temperature=$tempText " +
                    "reasoning_effort=$reasoningText reasoning_exclude=$excludeText stream=$stream"
            )
            val request = VlmProviderRequest(
                modelId = profile.modelId,
                messages = attemptMessages,
                options = plan.options,
                family = family
            )
            val result = if (stream && callback != null) {
                provider.sendChatStreaming(request, callback)
            } else {
                provider.sendChat(request)
            }
            lastResult = result
            val parsed = result.parsed
            AppLogger.d(
                VLM_LOG_TAG,
                "VLM extract: path=${parsed.debugPath} contentType=${parsed.contentType} " +
                    "reasoning=${parsed.debugReasoningSummary != null} reasoningOnly=${parsed.isReasoningOnly}"
            )
            if (!parsed.isReasoningOnly && parsed.finalAnswer.isNotBlank()) {
                return@withContext toClientResult(parsed, result, attempt, plan.label)
            }
            if (parsed.isReasoningOnly) {
                AppLogger.w(
                    VLM_LOG_TAG,
                    "VLM: Reasoning-only response (attempt=${plan.label}); retry=${attempt < retryPlans.size - 1}"
                )
                attempt += 1
                continue
            }
            AppLogger.w(VLM_LOG_TAG, "VLM: Empty assistant content (attempt=${plan.label})")
            break
        }
        val fallbackParsed = lastResult?.parsed
        if (fallbackParsed != null) {
            return@withContext toClientResult(fallbackParsed, lastResult, attempt, lastPlanLabel)
        }
        VlmClientResult(
            assistantContent = "",
            rawResponse = "",
            requestId = null,
            debugPath = "no_response",
            reasoning = null,
            reasoningDetailsJson = null,
            isReasoningOnly = false,
            retries = attempt
        )
    }

    fun updateProfile(newProfile: VlmProfile) {
        profile = newProfile
    }

    private fun resolveTokenPolicy(
        profile: VlmProfile,
        maxTokensOverride: Int,
        family: VlmModelFamily
    ): VlmTokenPolicy {
        val defaults = VlmModelFamilyPolicy.defaultTokenPolicy(family)
        val baseMax = when {
            maxTokensOverride > 0 -> maxTokensOverride
            profile.tokenPolicy.maxTokens > 0 -> profile.tokenPolicy.maxTokens
            else -> defaults.maxTokens
        }
        return VlmTokenPolicy(
            maxTokens = baseMax,
            reasoningEffort = profile.tokenPolicy.reasoningEffort ?: defaults.reasoningEffort,
            reasoningExclude = profile.tokenPolicy.reasoningExclude,
            retry1MaxTokens = profile.tokenPolicy.retry1MaxTokens ?: defaults.retry1MaxTokens,
            retry2MaxTokens = profile.tokenPolicy.retry2MaxTokens ?: defaults.retry2MaxTokens
        )
    }

    private fun resolveTemperature(profile: VlmProfile, temperatureOverride: Double): Double? {
        return when {
            temperatureOverride >= 0.0 -> temperatureOverride
            profile.parameterOverrides.temperature != null -> profile.parameterOverrides.temperature
            else -> null
        }
    }

    private fun addFinalOnlyInstruction(messages: List<VlmChatMessage>): List<VlmChatMessage> {
        val instruction = VlmChatMessage(
            role = "system",
            content = listOf(VlmContentPart.Text(FINAL_ONLY_INSTRUCTION))
        )
        val systemIndex = messages.indexOfFirst { it.role == "system" }
        if (systemIndex < 0) {
            return listOf(instruction) + messages
        }
        val updated = messages.toMutableList()
        updated.add(systemIndex + 1, instruction)
        return updated
    }

    private fun toClientResult(
        parsed: VlmParsedResponse,
        result: VlmProviderResult,
        retries: Int,
        retryLabel: String?
    ): VlmClientResult {
        return VlmClientResult(
            assistantContent = parsed.finalAnswer,
            rawResponse = result.rawResponse,
            requestId = result.requestId,
            debugPath = parsed.debugPath,
            reasoning = parsed.debugReasoningSummary,
            reasoningDetailsJson = parsed.reasoningDetailsJson,
            isReasoningOnly = parsed.isReasoningOnly,
            usage = parsed.usage,
            finishReason = parsed.finishReason,
            nativeFinishReason = parsed.nativeFinishReason,
            retries = retries,
            retryLabel = retryLabel
        )
    }
}
