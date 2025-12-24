package com.example.bikeassist.vlm

import com.example.bikeassist.util.AppLogger
import com.example.bikeassist.util.logLong
import com.example.bikeassist.util.truncateForLog
import com.example.bikebuddy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val VLM_LOG_TAG = "VLM"

class OpenRouterVlmClient(
    private val config: VlmConfig,
    private var profile: VlmProfile,
    private val apiKey: String = BuildConfig.OPENROUTER_API_KEY,
    private val endpoint: String = "https://openrouter.ai/api/v1/chat/completions"
) : VlmClient {

    override val isConfigured: Boolean = apiKey.isNotBlank()
    private val logTag = "VLM"

    override suspend fun chat(
        messages: List<VlmChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw IllegalStateException("OPENROUTER_API_KEY fehlt.")
        }
        val resolvedModel = profile.model.ifBlank { config.model }
        val profileMaxTokens = profile.maxTokens.takeIf { it > 0 } ?: config.maxTokens
        val resolvedMaxTokens = if (maxTokens > 0) maxTokens else profileMaxTokens
        val isGpt5Model = isGpt5Model(resolvedModel)
        val resolvedTemperature = if (isGpt5Model) {
            null
        } else {
            when {
                temperature >= 0.0 -> temperature
                profile.temperature != null -> profile.temperature
                else -> config.temperature
            }
        }
        val payload = JSONObject()
            .put("model", resolvedModel)
            .put("messages", buildMessagesJson(messages))
            .put("max_tokens", resolvedMaxTokens)
            .put("stream", false)
        if (!isGpt5Model && resolvedTemperature != null) {
            payload.put("temperature", resolvedTemperature)
        }
        if (isGpt5Model && !profile.thinkingEffort.isNullOrBlank()) {
            payload.put("reasoning", JSONObject().put("effort", profile.thinkingEffort))
        }
        val tempText = resolvedTemperature?.toString() ?: "unset"
        val reasoningText = if (isGpt5Model) profile.thinkingEffort ?: "unset" else "n/a"
        AppLogger.d(
            logTag,
            "OpenRouter request: model=$resolvedModel profile=${profile.id} endpoint=$endpoint " +
                "max_tokens=$resolvedMaxTokens temperature=$tempText reasoning_effort=$reasoningText"
        )

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("HTTP-Referer", "https://localhost")
            setRequestProperty("X-Title", "BikeBuddy")
        }

        connection.outputStream.use { out ->
            out.write(payload.toString().toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        val root = parseRoot(body, resolvedModel)
        val requestId = extractRequestId(root)
        val bodyLength = body.length

        if (code !in 200..299) {
            val msg = "OpenRouter HTTP $code (model=$resolvedModel requestId=${requestId ?: "-"})"
            AppLogger.e(logTag, "OpenRouter error: $msg bodyLength=$bodyLength")
            AppLogger.e(logTag, "OpenRouter error shape: ${summarizeJsonShape(root)}")
            AppLogger.e(logTag, "OpenRouter error body head=${body.take(1200).truncateForLog(1200)}")
            if (body.length > 1200) {
                AppLogger.e(logTag, "OpenRouter error body tail=${body.takeLast(1200).truncateForLog(1200)}")
            }
            logLong(logTag, "OpenRouter error body: ", body)
            val ex = IOException("$msg: ${body.truncateForLog(500)}")
            AppLogger.e(logTag, "VLM: OpenRouter HTTP error", ex)
            throw ex
        }
        AppLogger.d(
            logTag,
            "OpenRouter response: HTTP $code requestId=${requestId ?: "-"} model=$resolvedModel bodyLength=$bodyLength"
        )
        AppLogger.d(logTag, "OpenRouter response shape: ${summarizeJsonShape(root)}")
        logLong(logTag, "OpenRouter body: ", body)

        val extracted = extractAssistant(root, body, resolvedModel, requestId)
        AppLogger.d(
            logTag,
            "VLM extract: path=${extracted.debugPath} contentType=${extracted.contentType} " +
                "reasoning=${extracted.reasoning != null} reasoningDetails=${extracted.reasoningDetailsJson != null} " +
                "reasoningOnly=${extracted.isReasoningOnly}"
        )
        if (extracted.text.startsWith("<NO_TEXT_EXTRACTED>")) {
            AppLogger.w(logTag, "VLM: No assistant text extracted, using raw fallback")
        }
        VlmClientResult(
            assistantContent = extracted.text,
            rawResponse = body,
            requestId = requestId,
            debugPath = extracted.debugPath,
            reasoning = extracted.reasoning,
            reasoningDetailsJson = extracted.reasoningDetailsJson,
            isReasoningOnly = extracted.isReasoningOnly
        )
    }

    private fun buildMessagesJson(messages: List<VlmChatMessage>): JSONArray {
        val array = JSONArray()
        for (message in messages) {
            val msg = JSONObject().put("role", message.role)
            val contentArray = JSONArray()
            for (part in message.content) {
                when (part) {
                    is VlmContentPart.Text -> {
                        contentArray.put(JSONObject().put("type", "text").put("text", part.text))
                    }
                    is VlmContentPart.ImageUrl -> {
                        val imageObj = JSONObject().put("url", part.url)
                        contentArray.put(JSONObject().put("type", "image_url").put("image_url", imageObj))
                    }
                }
            }
            msg.put("content", contentArray)
            array.put(msg)
        }
        return array
    }

    private fun extractRequestId(root: JSONObject?): String? {
        return root?.optString("id", "")?.ifEmpty { null }
    }

    private fun extractErrorMessage(body: String): String? {
        return runCatching {
            val root = JSONObject(body)
            val errorObj = root.optJSONObject("error")
            val errorMessage = errorObj?.optString("message")?.takeIf { it.isNotBlank() }
            val errorType = errorObj?.optString("type")?.takeIf { it.isNotBlank() }
            when {
                errorMessage != null && errorType != null -> "$errorMessage (type=$errorType)"
                errorMessage != null -> errorMessage
                else -> root.optString("message", "").ifBlank { null }
            }
        }.getOrNull()
    }

    private fun isGpt5Model(model: String): Boolean {
        return model.startsWith("openai/gpt-5", ignoreCase = true)
    }

    fun updateProfile(newProfile: VlmProfile) {
        profile = newProfile
    }
}

internal data class ExtractedAssistant(
    val text: String,
    val reasoning: String?,
    val reasoningDetailsJson: String?,
    val debugPath: String,
    val contentType: String,
    val isReasoningOnly: Boolean
)

internal fun extractAssistant(
    root: JSONObject?,
    body: String,
    model: String,
    requestId: String?
): ExtractedAssistant {
    if (root == null) {
        AppLogger.e(
            VLM_LOG_TAG,
            "VLM: extractAssistant JSON parse failed model=$model requestId=${requestId ?: "-"} " +
                "body=${body.truncateForLog(200)}"
        )
        return ExtractedAssistant(
            text = "<NO_TEXT_EXTRACTED>\n${body.truncateForLog(800)}",
            reasoning = null,
            reasoningDetailsJson = null,
            debugPath = "root_parse_failed|raw_fallback",
            contentType = "unknown",
            isReasoningOnly = false
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
    val reasoning = message?.optString("reasoning")?.trim().takeIf { !it.isNullOrBlank() }
    val reasoningDetailsJson = message?.optJSONArray("reasoning_details")?.toString()
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
    var isReasoningOnly = false

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
    if (text.isBlank() && !reasoning.isNullOrBlank()) {
        text = reasoning
        debugPath = "message.reasoning"
        isReasoningOnly = true
    }
    if (text.isBlank() && !reasoningDetailsJson.isNullOrBlank()) {
        val detailsText = extractReasoningDetails(message?.optJSONArray("reasoning_details"))
        if (detailsText.isNotBlank()) {
            text = detailsText
            debugPath = "message.reasoning_details"
            isReasoningOnly = true
        }
    }
    if (text.isBlank()) {
        text = "<NO_TEXT_EXTRACTED>\n${body.truncateForLog(800)}"
        debugPath = "$debugPath|raw_fallback"
    }
    return ExtractedAssistant(
        text = text,
        reasoning = reasoning,
        reasoningDetailsJson = reasoningDetailsJson,
        debugPath = debugPath,
        contentType = contentType,
        isReasoningOnly = isReasoningOnly
    )
}

internal fun extractAssistant(body: String, model: String, requestId: String?): ExtractedAssistant {
    val root = parseRoot(body, model)
    return extractAssistant(root, body, model, requestId)
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

private fun summarizeJsonShape(root: JSONObject?): String {
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

private fun jsonKeys(obj: JSONObject?): String {
    if (obj == null) return "null"
    val keys = obj.keys().asSequence().toList().sorted()
    return keys.joinToString(prefix = "[", postfix = "]")
}

private fun parseRoot(body: String, model: String): JSONObject? {
    return runCatching { JSONObject(body) }.getOrElse { ex ->
        AppLogger.e(
            VLM_LOG_TAG,
            "VLM: response JSON parse failed model=$model body=${body.truncateForLog(200)}",
            ex
        )
        null
    }
}

private fun extractReasoningDetails(details: JSONArray?): String {
    if (details == null) return ""
    val sb = StringBuilder()
    for (i in 0 until details.length()) {
        val item = details.opt(i)
        val text = when (item) {
            is String -> item
            is JSONObject -> {
                item.optString("text").ifBlank {
                    item.optString("content").ifBlank {
                        item.optString("summary")
                    }
                }
            }
            else -> item.toString()
        }
        if (text.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(text)
        }
    }
    return sb.toString()
}
