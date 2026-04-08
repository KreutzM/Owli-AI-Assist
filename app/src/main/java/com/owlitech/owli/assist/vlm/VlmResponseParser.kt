package com.owlitech.owli.assist.vlm

import com.owlitech.owli.assist.util.AppLogger
import com.owlitech.owli.assist.util.truncateForLog
import org.json.JSONArray
import org.json.JSONObject

data class VlmUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val reasoningTokens: Int?
)

data class VlmParsedResponse(
    val finalAnswer: String,
    val debugPath: String,
    val contentType: String,
    val debugReasoningSummary: String?,
    val reasoningDetailsJson: String?,
    val isReasoningOnly: Boolean,
    val usage: VlmUsage?,
    val finishReason: String?,
    val nativeFinishReason: String?
)

object VlmResponseParser {
    private const val LOG_TAG = "VLM"

    fun parse(body: String, modelId: String, requestId: String?): VlmParsedResponse {
        val root = parseRoot(body, modelId)
        if (root == null) {
            AppLogger.e(
                LOG_TAG,
                "VLM: response JSON parse failed model=$modelId requestId=${requestId ?: "-"}"
            )
            return VlmParsedResponse(
                finalAnswer = "<NO_TEXT_EXTRACTED>\n${body.truncateForLog(800)}",
                debugPath = "root_parse_failed|raw_fallback",
                contentType = "unknown",
                debugReasoningSummary = null,
                reasoningDetailsJson = null,
                isReasoningOnly = false,
                usage = null,
                finishReason = null,
                nativeFinishReason = null
            )
        }
        val choice0 = root.optJSONArray("choices")?.optJSONObject(0)
        val message = choice0?.optJSONObject("message")
        val content = message?.opt("content")
        val contentType = when (content) {
            is String -> "string"
            is JSONArray -> "array"
            is JSONObject -> "object"
            null -> "null"
            else -> content::class.java.simpleName
        }
        val extractedFromContent = extractTextFromContent(content)
        val reasoningRaw = message?.optString("reasoning")?.trim().takeIf { !it.isNullOrBlank() }
        val reasoningDetails = message?.optJSONArray("reasoning_details")
        val reasoningDetailsJson = reasoningDetails?.toString()
        val reasoningSummary = extractReasoningSummary(reasoningDetails) ?: reasoningRaw
        var text = extractedFromContent.text
        var debugPath = buildString {
            append("message.content:")
            append(contentType)
            if (extractedFromContent.summary.isNotBlank()) {
                append("[")
                append(extractedFromContent.summary)
                append("]")
            }
        }

        if (text.isBlank()) {
            val messageOutputText = message?.optString("output_text")?.trim().takeIf { !it.isNullOrBlank() }
            if (messageOutputText != null) {
                text = messageOutputText
                debugPath = "message.output_text"
            }
        }
        if (text.isBlank()) {
            val messageText = message?.optString("text")?.trim().takeIf { !it.isNullOrBlank() }
            if (messageText != null) {
                text = messageText
                debugPath = "message.text"
            }
        }
        if (text.isBlank()) {
            val choiceText = choice0?.optString("text")?.trim().takeIf { !it.isNullOrBlank() }
            if (choiceText != null) {
                text = choiceText
                debugPath = "choice.text"
            }
        }
        if (text.isBlank()) {
            val rootOutputText = root.optString("output_text").trim().takeIf { it.isNotBlank() }
            if (rootOutputText != null) {
                text = rootOutputText
                debugPath = "root.output_text"
            }
        }
        if (text.isBlank()) {
            val outputExtracted = extractTextFromContent(root.optJSONArray("output"))
            if (outputExtracted.text.isNotBlank()) {
                text = outputExtracted.text
                debugPath = if (outputExtracted.summary.isNotBlank()) {
                    "root.output[${outputExtracted.summary}]"
                } else {
                    "root.output"
                }
            }
        }

        val isReasoningOnly = text.isBlank() && !reasoningSummary.isNullOrBlank()
        if (text.isBlank() && !isReasoningOnly) {
            text = "<NO_TEXT_EXTRACTED>\n${body.truncateForLog(800)}"
            debugPath = "$debugPath|raw_fallback"
        }
        val usage = parseUsage(root)
        val finishReason = choice0?.optString("finish_reason")?.ifBlank { null }
        val nativeFinishReason = choice0?.optString("native_finish_reason")?.ifBlank { null }
        return VlmParsedResponse(
            finalAnswer = if (isReasoningOnly) "" else text,
            debugPath = debugPath,
            contentType = contentType,
            debugReasoningSummary = reasoningSummary,
            reasoningDetailsJson = reasoningDetailsJson,
            isReasoningOnly = isReasoningOnly,
            usage = usage,
            finishReason = finishReason,
            nativeFinishReason = nativeFinishReason
        )
    }

    fun summarizeJsonShape(root: JSONObject?): String {
        if (root == null) return "shape: non-JSON"
        val choice0 = root.optJSONArray("choices")?.optJSONObject(0)
        val message = choice0?.optJSONObject("message")
        val content = message?.opt("content")
        val contentType = when (content) {
            is String -> "string"
            is JSONArray -> "array"
            is JSONObject -> "object"
            null -> "null"
            else -> content::class.java.simpleName
        }
        val firstContent = if (content is JSONArray) content.opt(0) else null
        val firstContentKeys = when (firstContent) {
            is JSONObject -> jsonKeys(firstContent)
            is String -> "string"
            null -> "null"
            else -> firstContent::class.java.simpleName
        }
        val hasReasoning = message?.optString("reasoning")?.isNotBlank() == true
        val hasReasoningDetails = message?.optJSONArray("reasoning_details")?.length()?.let { it > 0 } == true
        return "rootKeys=${jsonKeys(root)} choiceKeys=${jsonKeys(choice0)} messageKeys=${jsonKeys(message)} " +
            "contentType=$contentType firstContentKeys=$firstContentKeys " +
            "reasoning=$hasReasoning reasoningDetails=$hasReasoningDetails"
    }

    private fun parseRoot(body: String, modelId: String): JSONObject? {
        return runCatching { JSONObject(body) }.getOrElse { ex ->
            AppLogger.e(LOG_TAG, "VLM: response JSON parse failed model=$modelId", ex)
            null
        }
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

    private data class ExtractedText(
        val text: String,
        val summary: String
    )

    private fun extractTextFromContent(content: Any?): ExtractedText {
        val counts = linkedMapOf<String, Int>()
        val sb = StringBuilder()
        fun count(label: String) {
            counts[label] = (counts[label] ?: 0) + 1
        }
        fun appendSegment(segment: String) {
            if (segment.isBlank()) return
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(segment)
        }
        when (content) {
            is String -> {
                count("string")
                appendSegment(content)
            }
            is JSONObject -> {
                val type = content.optString("type").trim()
                if (type == "image_url") {
                    count("image_url")
                    return ExtractedText(text = "", summary = counts.entries.joinToString(",") { "${it.key}:${it.value}" })
                }
                val label = type.ifBlank {
                    when {
                        content.has("text") -> "text"
                        content.has("content") -> "content"
                        else -> "object"
                    }
                }
                count(label)
                val text = content.optString("text").ifBlank { content.optString("content") }
                appendSegment(text)
            }
            is JSONArray -> {
                for (i in 0 until content.length()) {
                    val item = content.opt(i)
                    when (item) {
                        is String -> {
                            count("string")
                            appendSegment(item)
                        }
                        is JSONObject -> {
                            val type = item.optString("type").trim()
                            if (type == "image_url") {
                                count("image_url")
                                continue
                            }
                            val label = type.ifBlank {
                                when {
                                    item.has("text") -> "text"
                                    item.has("content") -> "content"
                                    else -> "object"
                                }
                            }
                            count(label)
                            val text = item.optString("text").ifBlank { item.optString("content") }
                            appendSegment(text)
                        }
                        is JSONArray -> {
                            count("array")
                        }
                        else -> {
                            count("unknown")
                        }
                    }
                }
            }
        }
        val summary = counts.entries.joinToString(",") { "${it.key}:${it.value}" }
        return ExtractedText(text = sb.toString(), summary = summary)
    }

    private fun extractReasoningSummary(details: JSONArray?): String? {
        if (details == null) return null
        var fallback: String? = null
        for (i in 0 until details.length()) {
            val item = details.opt(i)
            when (item) {
                is JSONObject -> {
                    val type = item.optString("type").trim().lowercase()
                    val text = item.optString("text").ifBlank {
                        item.optString("summary").ifBlank {
                            item.optString("content")
                        }
                    }
                    if (type == "reasoning.summary" && text.isNotBlank()) {
                        return text
                    }
                    if (fallback == null && text.isNotBlank()) {
                        fallback = text
                    }
                }
                is String -> {
                    if (fallback == null && item.isNotBlank()) {
                        fallback = item
                    }
                }
            }
        }
        return fallback
    }

    private fun jsonKeys(obj: JSONObject?): String {
        if (obj == null) return "null"
        val keys = obj.keys().asSequence().toList().sorted()
        return keys.joinToString(prefix = "[", postfix = "]")
    }
}
