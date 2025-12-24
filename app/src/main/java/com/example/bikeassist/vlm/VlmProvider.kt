package com.example.bikeassist.vlm

data class VlmRequestOptions(
    val maxTokens: Int,
    val temperature: Double?,
    val reasoningEffort: String?,
    val includeReasoning: Boolean
)

data class VlmProviderRequest(
    val modelId: String,
    val messages: List<VlmChatMessage>,
    val options: VlmRequestOptions,
    val family: VlmModelFamily
)

data class VlmProviderResult(
    val parsed: VlmParsedResponse,
    val rawResponse: String,
    val requestId: String?,
    val httpCode: Int,
    val payloadOmittedFields: List<String>
)

interface VlmProvider {
    suspend fun sendChat(request: VlmProviderRequest): VlmProviderResult
}
