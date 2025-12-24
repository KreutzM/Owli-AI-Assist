package com.example.bikeassist.vlm

sealed class VlmContentPart {
    data class Text(val text: String) : VlmContentPart()
    data class ImageUrl(val url: String) : VlmContentPart()
}

data class VlmChatMessage(
    val role: String,
    val content: List<VlmContentPart>
)

data class VlmClientResult(
    val assistantContent: String,
    val rawResponse: String,
    val requestId: String? = null,
    val debugPath: String? = null,
    val reasoning: String? = null,
    val reasoningDetailsJson: String? = null,
    val isReasoningOnly: Boolean = false,
    val usage: VlmUsage? = null,
    val finishReason: String? = null,
    val nativeFinishReason: String? = null,
    val retries: Int = 0,
    val retryLabel: String? = null
)

interface VlmClient {
    val isConfigured: Boolean

    suspend fun chat(
        messages: List<VlmChatMessage>,
        maxTokens: Int = -1,
        temperature: Double = -1.0
    ): VlmClientResult
}
