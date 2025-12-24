package com.example.bikeassist.vlm

import com.example.bikeassist.util.AppLogger
import com.example.bikebuddy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterVlmClient(
    private val config: VlmConfig,
    private var profile: VlmProfile,
    private val apiKey: String = BuildConfig.OPENROUTER_API_KEY,
    private val endpoint: String = "https://openrouter.ai/api/v1/chat/completions"
) : VlmClient {

    override val isConfigured: Boolean = apiKey.isNotBlank()

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
        val profileTemperature = if (!profile.temperature.isNaN()) profile.temperature else config.temperature
        val resolvedMaxTokens = if (maxTokens > 0) maxTokens else profileMaxTokens
        val resolvedTemperature = if (temperature >= 0.0) temperature else profileTemperature
        val payload = JSONObject()
            .put("model", resolvedModel)
            .put("messages", buildMessagesJson(messages))
            .put("temperature", resolvedTemperature)
            .put("max_tokens", resolvedMaxTokens)
            .put("stream", false)
        if (profile.thinkingEnabled && profile.thinkingBudgetTokens != null) {
            // TODO: "thinking" payload format in OpenRouter klären, dann hier setzen.
        }

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

        if (code !in 200..299) {
            throw IOException("OpenRouter HTTP $code: ${body.take(500)}")
        }

        val assistant = parseAssistantContent(body)
        if (assistant.isBlank()) {
            val error = extractErrorMessage(body)
            if (!error.isNullOrBlank()) {
                throw IOException("OpenRouter Fehler: $error")
            }
            throw IOException("Leere VLM-Antwort. raw=${body.take(500)}")
        }
        VlmClientResult(
            assistantContent = assistant,
            rawResponse = body,
            requestId = extractRequestId(body)
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

    private fun parseAssistantContent(body: String): String {
        return try {
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: return ""
            val first = choices.optJSONObject(0) ?: return ""
            val message = first.optJSONObject("message")
            val content = message?.opt("content") ?: first.opt("text")
            when (content) {
                is String -> content
                is JSONArray -> {
                    val sb = StringBuilder()
                    for (i in 0 until content.length()) {
                        val item = content.optJSONObject(i) ?: continue
                        val type = item.optString("type")
                        if (type == "text") {
                            sb.append(item.optString("text"))
                        }
                    }
                    sb.toString()
                }
                is JSONObject -> {
                    val text = content.optString("text", "")
                    if (text.isNotBlank()) {
                        text
                    } else {
                        content.optString("content", "")
                    }
                }
                else -> ""
            }
        } catch (ex: Exception) {
            AppLogger.e(ex, "Failed to parse assistant content")
            ""
        }
    }

    private fun extractRequestId(body: String): String? {
        return runCatching { JSONObject(body).optString("id", "") }
            .getOrNull()
            ?.ifEmpty { null }
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

    fun updateProfile(newProfile: VlmProfile) {
        profile = newProfile
    }
}
