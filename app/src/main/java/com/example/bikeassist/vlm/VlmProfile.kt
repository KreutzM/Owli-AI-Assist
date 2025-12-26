package com.example.bikeassist.vlm

data class VlmCapabilities(
    val supportsVision: Boolean = true,
    val supportsReasoning: Boolean = false,
    val supportsJson: Boolean = false
)

data class VlmImageSettings(
    val maxSidePx: Int = 1024,
    val jpegQuality: Int = 80,
    val detail: String? = null
)

data class VlmTokenPolicy(
    val maxTokens: Int,
    val reasoningEffort: String? = null,
    val reasoningExclude: Boolean = false,
    val retry1MaxTokens: Int? = null,
    val retry2MaxTokens: Int? = null
)

data class VlmParameterOverrides(
    val temperature: Double? = null,
    val reasoningEffort: String? = null
)

data class VlmAutoScanConfig(
    val enabledByDefault: Boolean = false,
    val intervalMs: Long = 2000L,
    val speakFreeEveryMs: Long = 10000L
)

data class VlmProfile(
    val id: String,
    val label: String,
    val description: String?,
    val modelId: String,
    val provider: String,
    val family: String?,
    val systemPrompt: String,
    val overviewPrompt: String,
    val imageSettings: VlmImageSettings,
    val tokenPolicy: VlmTokenPolicy,
    val parameterOverrides: VlmParameterOverrides,
    val capabilities: VlmCapabilities,
    val streamingEnabled: Boolean = false,
    val autoScan: VlmAutoScanConfig? = null
)
