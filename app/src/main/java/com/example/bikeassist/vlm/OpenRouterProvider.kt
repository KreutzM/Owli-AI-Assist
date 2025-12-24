package com.example.bikeassist.vlm

import com.example.bikeassist.util.AppLogger
import com.example.bikeassist.util.logLong
import com.example.bikeassist.util.truncateForLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val VLM_LOG_TAG = "VLM"

class OpenRouterProvider(
    private val apiKey: String,
    private val endpoint: String = "https://openrouter.ai/api/v1/chat/completions"
) : VlmProvider {

    override suspend fun sendChat(request: VlmProviderRequest): VlmProviderResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OPENROUTER_API_KEY fehlt.")
        }
        val payloadResult = buildPayload(request)
        val payload = payloadResult.payload
        val payloadForLog = redactPayloadForLog(payload)
        AppLogger.d(
            VLM_LOG_TAG,
            "OpenRouter payload: model=${request.modelId} omitted=${payloadResult.omittedFields.joinToString()} " +
                "payload=${payloadForLog.truncateForLog(1200)}"
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
        val root = parseRoot(body, request.modelId)
        val requestId = root?.optString("id", "")?.ifEmpty { null }
        val bodyLength = body.length

        if (code !in 200..299) {
            val msg = "OpenRouter HTTP $code (model=${request.modelId} requestId=${requestId ?: "-"})"
            AppLogger.e(VLM_LOG_TAG, "OpenRouter error: $msg bodyLength=$bodyLength")
            AppLogger.e(VLM_LOG_TAG, "OpenRouter error shape: ${VlmResponseParser.summarizeJsonShape(root)}")
            AppLogger.e(VLM_LOG_TAG, "OpenRouter error body head=${body.take(1200).truncateForLog(1200)}")
            if (body.length > 1200) {
                AppLogger.e(VLM_LOG_TAG, "OpenRouter error body tail=${body.takeLast(1200).truncateForLog(1200)}")
            }
            logLong(VLM_LOG_TAG, "OpenRouter error body: ", body)
            val ex = IOException("$msg: ${body.truncateForLog(500)}")
            AppLogger.e(VLM_LOG_TAG, "VLM: OpenRouter HTTP error", ex)
            throw ex
        }
        AppLogger.d(
            VLM_LOG_TAG,
            "OpenRouter response: HTTP $code requestId=${requestId ?: "-"} model=${request.modelId} bodyLength=$bodyLength"
        )
        AppLogger.d(VLM_LOG_TAG, "OpenRouter response shape: ${VlmResponseParser.summarizeJsonShape(root)}")
        logLong(VLM_LOG_TAG, "OpenRouter body: ", body)

        val parsed = VlmResponseParser.parse(body, request.modelId, requestId)
        val usage = parsed.usage?.let {
            "usage prompt=${it.promptTokens} completion=${it.completionTokens} reasoning=${it.reasoningTokens ?: "-"}"
        } ?: "usage n/a"
        val finish = parsed.finishReason ?: "-"
        val nativeFinish = parsed.nativeFinishReason ?: "-"
        AppLogger.d(VLM_LOG_TAG, "OpenRouter usage: $usage finish=$finish nativeFinish=$nativeFinish")

        VlmProviderResult(
            parsed = parsed,
            rawResponse = body,
            requestId = requestId,
            httpCode = code,
            payloadOmittedFields = payloadResult.omittedFields
        )
    }

    private fun buildPayload(request: VlmProviderRequest): PayloadBuildResult {
        val payload = JSONObject()
            .put("model", request.modelId)
            .put("messages", buildMessagesJson(request.messages))
            .put("max_tokens", request.options.maxTokens)
            .put("stream", false)

        val omitted = mutableListOf<String>()
        if (VlmModelFamilyPolicy.allowTemperature(request.family)) {
            val temperature = request.options.temperature
            if (temperature != null) {
                payload.put("temperature", temperature)
            } else {
                omitted += "temperature(unset)"
            }
        } else if (request.options.temperature != null) {
            omitted += "temperature(disallowed)"
        }

        val reasoningEffort = request.options.reasoningEffort
        if (VlmModelFamilyPolicy.allowReasoning(request.family) && request.options.includeReasoning) {
            if (!reasoningEffort.isNullOrBlank()) {
                payload.put("reasoning", JSONObject().put("effort", reasoningEffort))
            } else {
                omitted += "reasoning(unset)"
            }
        } else if (!reasoningEffort.isNullOrBlank()) {
            omitted += "reasoning(disallowed)"
        }

        return PayloadBuildResult(payload = payload, omittedFields = omitted)
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

    private fun redactPayloadForLog(payload: JSONObject): String {
        val clone = JSONObject(payload.toString())
        val messages = clone.optJSONArray("messages") ?: return clone.toString()
        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            val content = msg.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val item = content.optJSONObject(j) ?: continue
                when (item.optString("type")) {
                    "image_url" -> {
                        val image = item.optJSONObject("image_url") ?: continue
                        val url = image.optString("url")
                        val redacted = if (url.startsWith("data:image")) {
                            val length = url.length
                            "data:image/<redacted len=$length>"
                        } else if (url.isNotBlank()) {
                            "<redacted>"
                        } else {
                            url
                        }
                        image.put("url", redacted)
                    }
                    "text" -> {
                        val text = item.optString("text")
                        if (text.length > 200) {
                            item.put("text", text.truncateForLog(200))
                        }
                    }
                }
            }
        }
        return clone.toString()
    }

    private fun parseRoot(body: String, model: String): JSONObject? {
        return runCatching { JSONObject(body) }.getOrElse { ex ->
            AppLogger.e(VLM_LOG_TAG, "VLM: response JSON parse failed model=$model body=${body.truncateForLog(200)}", ex)
            null
        }
    }

    private data class PayloadBuildResult(
        val payload: JSONObject,
        val omittedFields: List<String>
    )
}
