package com.example.bikeassist.vlm

import org.json.JSONObject

internal data class VlmStreamEvent(
    val deltaText: String?,
    val reasoningDelta: String?,
    val finishReason: String?,
    val nativeFinishReason: String?,
    val usage: VlmUsage?,
    val requestId: String?
)

internal object VlmSseParser {
    fun parseEvent(data: String): VlmStreamEvent? {
        val root = runCatching { JSONObject(data) }.getOrNull() ?: return null
        val requestId = root.optString("id", "").ifBlank { null }
        val choice0 = root.optJSONArray("choices")?.optJSONObject(0)
        val delta = choice0?.optJSONObject("delta")
        val deltaText = delta?.optString("content")?.takeIf { it.isNotBlank() }
            ?: delta?.optString("text")?.takeIf { it.isNotBlank() }
        val reasoningDelta = delta?.optString("reasoning")?.takeIf { it.isNotBlank() }
        val finishReason = choice0?.optString("finish_reason")?.ifBlank { null }
        val nativeFinishReason = choice0?.optString("native_finish_reason")?.ifBlank { null }
        val usage = parseUsage(root)
        return VlmStreamEvent(
            deltaText = deltaText,
            reasoningDelta = reasoningDelta,
            finishReason = finishReason,
            nativeFinishReason = nativeFinishReason,
            usage = usage,
            requestId = requestId
        )
    }

    private fun parseUsage(root: JSONObject): VlmUsage? {
        val usage = root.optJSONObject("usage") ?: return null
        val promptTokens = usage.optInt("prompt_tokens", -1)
        val completionTokens = usage.optInt("completion_tokens", -1)
        if (promptTokens < 0 && completionTokens < 0) return null
        val reasoningTokens = usage.optJSONObject("completion_tokens_details")
            ?.optInt("reasoning_tokens", -1)
            ?.takeIf { it >= 0 }
        return VlmUsage(
            promptTokens = promptTokens.coerceAtLeast(0),
            completionTokens = completionTokens.coerceAtLeast(0),
            reasoningTokens = reasoningTokens
        )
    }
}
