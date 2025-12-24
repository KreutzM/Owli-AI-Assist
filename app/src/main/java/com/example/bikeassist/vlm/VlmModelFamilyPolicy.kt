package com.example.bikeassist.vlm

enum class VlmModelFamily {
    GPT5,
    GPT4O,
    OTHER
}

data class VlmRetryPlan(
    val label: String,
    val options: VlmRequestOptions,
    val addFinalOnlyInstruction: Boolean
)

object VlmModelFamilyPolicy {
    private const val GPT5_DEFAULT_MAX_TOKENS = 900
    private const val GPT5_RETRY1_MAX_TOKENS = 1200
    private const val GPT5_RETRY2_MAX_TOKENS = 1400
    private const val DEFAULT_MAX_TOKENS = 320

    private val allowedEfforts = setOf("xhigh", "high", "medium", "low", "minimal", "none")

    fun resolveFamily(profile: VlmProfile): VlmModelFamily {
        val explicit = profile.family?.trim()?.lowercase()
        if (!explicit.isNullOrBlank()) {
            return when (explicit) {
                "gpt5", "gpt-5" -> VlmModelFamily.GPT5
                "gpt4o", "gpt-4o", "4o" -> VlmModelFamily.GPT4O
                else -> VlmModelFamily.OTHER
            }
        }
        val modelId = profile.modelId.trim().lowercase()
        return when {
            modelId.startsWith("openai/gpt-5") -> VlmModelFamily.GPT5
            modelId.startsWith("openai/gpt-4o") -> VlmModelFamily.GPT4O
            else -> VlmModelFamily.OTHER
        }
    }

    fun allowTemperature(family: VlmModelFamily): Boolean {
        return family != VlmModelFamily.GPT5
    }

    fun allowReasoning(family: VlmModelFamily): Boolean {
        return family == VlmModelFamily.GPT5
    }

    fun defaultTokenPolicy(family: VlmModelFamily): VlmTokenPolicy {
        return when (family) {
            VlmModelFamily.GPT5 -> VlmTokenPolicy(
                maxTokens = GPT5_DEFAULT_MAX_TOKENS,
                reasoningEffort = "low",
                retry1MaxTokens = GPT5_RETRY1_MAX_TOKENS,
                retry2MaxTokens = GPT5_RETRY2_MAX_TOKENS
            )
            else -> VlmTokenPolicy(maxTokens = DEFAULT_MAX_TOKENS)
        }
    }

    fun sanitizeReasoningEffort(value: String?): String? {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized.takeIf { it.isNotBlank() && it in allowedEfforts }
    }

    fun buildRetryPlans(
        family: VlmModelFamily,
        baseOptions: VlmRequestOptions,
        tokenPolicy: VlmTokenPolicy
    ): List<VlmRetryPlan> {
        val plans = mutableListOf(VlmRetryPlan("base", baseOptions, addFinalOnlyInstruction = false))
        if (!allowReasoning(family)) {
            return plans
        }
        val defaultPolicy = defaultTokenPolicy(family)
        val retry1Max = resolveRetryMax(
            baseOptions.maxTokens,
            tokenPolicy.retry1MaxTokens ?: defaultPolicy.retry1MaxTokens
        )
        val retry2Max = resolveRetryMax(
            retry1Max,
            tokenPolicy.retry2MaxTokens ?: defaultPolicy.retry2MaxTokens
        )
        val retry1Options = baseOptions.copy(
            maxTokens = retry1Max,
            reasoningEffort = "low",
            includeReasoning = true
        )
        val retry2Options = baseOptions.copy(
            maxTokens = retry2Max,
            reasoningEffort = null,
            includeReasoning = false
        )
        plans += VlmRetryPlan("retry1_low_reasoning", retry1Options, addFinalOnlyInstruction = false)
        plans += VlmRetryPlan("retry2_no_reasoning", retry2Options, addFinalOnlyInstruction = true)
        return plans
    }

    private fun resolveRetryMax(base: Int, override: Int?): Int {
        val candidate = override ?: base
        return if (candidate > base) candidate else base
    }
}
