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
        val nonStreamRequest = request.copy(options = request.options.copy(stream = false))
        val payloadResult = buildPayload(nonStreamRequest)
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

    override suspend fun sendChatStreaming(
        request: VlmProviderRequest,
        callback: VlmStreamingCallback
    ): VlmProviderResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OPENROUTER_API_KEY fehlt.")
        }
        val streamRequest = request.copy(options = request.options.copy(stream = true))
        val payloadResult = buildPayload(streamRequest)
        val payload = payloadResult.payload
        val payloadForLog = redactPayloadForLog(payload)
        AppLogger.d(
            VLM_LOG_TAG,
            "OpenRouter payload(stream): model=${request.modelId} omitted=${payloadResult.omittedFields.joinToString()} " +
                "payload=${payloadForLog.truncateForLog(1200)}"
        )

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("HTTP-Referer", "https://localhost")
            setRequestProperty("X-Title", "BikeBuddy")
        }

        connection.outputStream.use { out ->
            out.write(payload.toString().toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        if (code !in 200..299) {
            val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = parseRoot(body, request.modelId)
            val requestId = root?.optString("id", "")?.ifEmpty { null }
            val msg = "OpenRouter HTTP $code (model=${request.modelId} requestId=${requestId ?: "-"})"
            AppLogger.e(VLM_LOG_TAG, "OpenRouter error: $msg bodyLength=${body.length}")
            AppLogger.e(VLM_LOG_TAG, "OpenRouter error shape: ${VlmResponseParser.summarizeJsonShape(root)}")
            logLong(VLM_LOG_TAG, "OpenRouter error body: ", body)
            val ex = IOException("$msg: ${body.truncateForLog(500)}")
            AppLogger.e(VLM_LOG_TAG, "VLM: OpenRouter HTTP error", ex)
            callback.onError(ex)
            throw ex
        }

        val reader = connection.inputStream.bufferedReader()
        val finalBuffer = StringBuilder()
        var finishReason: String? = null
        var nativeFinishReason: String? = null
        var usage: VlmUsage? = null
        var requestId: String? = null
        var sawReasoning = false

        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isBlank()) continue
                if (data == "[DONE]") {
                    break
                }
            val event = VlmSseParser.parseEvent(data) ?: continue
                if (requestId == null && event.requestId != null) {
                    requestId = event.requestId
                }
                if (event.deltaText?.isNotBlank() == true) {
                    finalBuffer.append(event.deltaText)
                    callback.onDelta(event.deltaText)
                }
                if (event.reasoningDelta?.isNotBlank() == true) {
                    sawReasoning = true
                }
                finishReason = event.finishReason ?: finishReason
                nativeFinishReason = event.nativeFinishReason ?: nativeFinishReason
                usage = event.usage ?: usage
            }
        } catch (ex: Exception) {
            AppLogger.e(ex, "VLM: Streaming parse failed")
            callback.onError(ex)
            throw ex
        } finally {
            reader.close()
        }

        val finalText = finalBuffer.toString()
        val isReasoningOnly = finalText.isBlank() && sawReasoning
        val parsed = VlmParsedResponse(
            finalAnswer = if (isReasoningOnly) "" else finalText,
            debugPath = "stream.delta",
            contentType = "stream",
            debugReasoningSummary = if (sawReasoning) "<stream>" else null,
            reasoningDetailsJson = null,
            isReasoningOnly = isReasoningOnly,
            usage = usage,
            finishReason = finishReason,
            nativeFinishReason = nativeFinishReason
        )
        callback.onComplete(finalText, usage, finishReason, nativeFinishReason)
        val usageText = usage?.let {
            "usage prompt=${it.promptTokens} completion=${it.completionTokens} reasoning=${it.reasoningTokens ?: "-"}"
        } ?: "usage n/a"
        AppLogger.d(VLM_LOG_TAG, "OpenRouter stream done: $usageText finish=${finishReason ?: "-"}")
        VlmProviderResult(
            parsed = parsed,
            rawResponse = "<stream>",
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
            .put("stream", request.options.stream)

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
        val reasoningExclude = request.options.reasoningExclude
        if (VlmModelFamilyPolicy.allowReasoning(request.family) && request.options.includeReasoning) {
            val reasoning = JSONObject()
            if (reasoningExclude) {
                reasoning.put("exclude", true)
            }
            if (!reasoningEffort.isNullOrBlank()) {
                reasoning.put("effort", reasoningEffort)
            }
            if (reasoning.length() > 0) {
                payload.put("reasoning", reasoning)
            } else {
                omitted += "reasoning(unset)"
            }
        } else if (reasoningExclude || !reasoningEffort.isNullOrBlank()) {
            omitted += "reasoning(disallowed)"
        }

        return PayloadBuildResult(payload = payload, omittedFields = omitted)
    }

    internal fun buildPayloadForTest(request: VlmProviderRequest): PayloadBuildResult {
        return buildPayload(request)
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

    internal data class PayloadBuildResult(
        val payload: JSONObject,
        val omittedFields: List<String>
    )
}
