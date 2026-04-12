package com.owlitech.owli.assist.vlm

import android.content.Context
import com.owlitech.owli.assist.BuildConfig
import com.owlitech.owli.assist.settings.VlmTransportMode
import com.owlitech.owli.assist.settings.VlmTransportSelection
import com.owlitech.owli.assist.settings.toOpenRouterApiKeySelection
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.UUID

private const val OWLI_BACKEND_BASE_URL = "https://api.owli-ai.com"
private const val OWLI_BACKEND_PREFS = "owli_backend_transport"
private const val INSTALLATION_ID_KEY = "installation_id"

data class OwliBackendBootstrapResponse(
    val sessionToken: String,
    val expiresAt: Instant?,
    val canDescribe: Boolean,
    val canFollowUp: Boolean
)

data class OwliBackendDescribeResponse(
    val answerText: String,
    val mode: String?,
    val modelAlias: String?,
    val requestId: String?,
    val sceneToken: String,
    val sceneTokenExpiresAt: Instant?
)

data class OwliBackendFollowUpResponse(
    val answerText: String,
    val mode: String?,
    val modelAlias: String?,
    val requestId: String?
)

internal object OwliBackendResponseParser {
    fun parseBootstrap(body: String): OwliBackendBootstrapResponse? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val sessionToken = root.optString("sessionToken").trim().ifEmpty { return null }
        return OwliBackendBootstrapResponse(
            sessionToken = sessionToken,
            expiresAt = root.optString("expiresAt").toInstantOrNull(),
            canDescribe = root.optJSONObject("featureFlags")?.optBoolean("sceneDescribe", true) ?: true,
            canFollowUp = root.optJSONObject("featureFlags")?.optBoolean("followup", true) ?: true
        )
    }

    fun parseDescribe(body: String): OwliBackendDescribeResponse? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val answerText = root.optString("answerText").trim().ifEmpty { return null }
        val sceneToken = root.optString("sceneToken").trim().ifEmpty { return null }
        return OwliBackendDescribeResponse(
            answerText = answerText,
            mode = root.optString("mode").trim().ifEmpty { null },
            modelAlias = root.optString("modelAlias").trim().ifEmpty { null },
            requestId = root.optString("requestId").trim().ifEmpty { null },
            sceneToken = sceneToken,
            sceneTokenExpiresAt = root.optString("sceneTokenExpiresAt").toInstantOrNull()
        )
    }

    fun parseFollowUp(body: String): OwliBackendFollowUpResponse? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val answerText = root.optString("answerText").trim().ifEmpty { return null }
        return OwliBackendFollowUpResponse(
            answerText = answerText,
            mode = root.optString("mode").trim().ifEmpty { null },
            modelAlias = root.optString("modelAlias").trim().ifEmpty { null },
            requestId = root.optString("requestId").trim().ifEmpty { null }
        )
    }

    fun parseErrorMessage(body: String): String? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val message = root.optString("message").trim()
        if (message.isNotEmpty()) {
            return message
        }
        val error = root.optString("error").trim()
        return error.ifEmpty { null }
    }

    private fun String?.toInstantOrNull(): Instant? {
        val value = this?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}

class OwliBackendInstallationIdStore(context: Context) {
    private val preferences = context.getSharedPreferences(OWLI_BACKEND_PREFS, Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        val existing = preferences.getString(INSTALLATION_ID_KEY, null)?.trim().orEmpty()
        if (existing.isNotEmpty()) {
            return existing
        }
        val created = UUID.randomUUID().toString()
        preferences.edit().putString(INSTALLATION_ID_KEY, created).apply()
        return created
    }
}

class OwliBackendVlmClient(
    private var profile: VlmProfile,
    private val baseUrl: String = OWLI_BACKEND_BASE_URL,
    private val installationIdProvider: () -> String,
    private val appVersionProvider: () -> String = { BuildConfig.VERSION_NAME },
    private val versionCodeProvider: () -> Int = { BuildConfig.VERSION_CODE },
    private val localeProvider: () -> String = { Locale.getDefault().toLanguageTag() },
    private val nowProvider: () -> Instant = { Instant.now() }
) : VlmClient {

    private var activeSession: OwliBackendBootstrapResponse? = null
    private var activeSceneToken: String? = null
    private var activeSceneTokenExpiresAt: Instant? = null

    override val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    override suspend fun chat(
        messages: List<VlmChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        return runRequest(messages)
    }

    override suspend fun chatStreaming(
        messages: List<VlmChatMessage>,
        callback: VlmStreamingCallback,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        val result = runRequest(messages)
        callback.onComplete(result.assistantContent, null, null, null)
        return result
    }

    fun updateProfile(newProfile: VlmProfile) {
        profile = newProfile
    }

    fun resetConversation() {
        activeSceneToken = null
        activeSceneTokenExpiresAt = null
    }

    private fun runRequest(messages: List<VlmChatMessage>): VlmClientResult {
        if (!isConfigured) {
            throw IOException("Owli backend transport is not configured.")
        }
        val latestUser = messages.lastOrNull { it.role == "user" }
            ?: throw IOException("No user request available for backend transport.")
        val describesScene = messages.none { it.role == "assistant" } &&
            latestUser.content.any { it is VlmContentPart.ImageUrl }

        return if (describesScene) {
            describeScene(latestUser)
        } else {
            followUp(latestUser)
        }
    }

    private fun describeScene(message: VlmChatMessage): VlmClientResult {
        val session = ensureSession()
        if (!session.canDescribe) {
            throw IOException("Scene describe is not enabled for the current backend session.")
        }
        val imageUrl = message.content.filterIsInstance<VlmContentPart.ImageUrl>().lastOrNull()?.url
            ?: throw IOException("Backend scene describe needs a snapshot image.")
        val dataUrl = parseDataUrl(imageUrl)
            ?: throw IOException("Snapshot image format is not supported for backend transport.")
        val requestBody = JSONObject()
            .put("sessionToken", session.sessionToken)
            .put("installationId", installationIdProvider())
            .put("imageBase64", dataUrl.base64)
            .put("imageMimeType", dataUrl.mimeType)
            .put("sceneMode", "describe")

        val (body, _) = postJson("$baseUrl/api/v1/scene/describe", requestBody)
        val response = OwliBackendResponseParser.parseDescribe(body)
            ?: throw IOException("Owli backend scene describe returned an invalid response.")
        activeSceneToken = response.sceneToken
        activeSceneTokenExpiresAt = response.sceneTokenExpiresAt
        return VlmClientResult(
            assistantContent = response.answerText,
            rawResponse = "<owli-backend-describe>",
            requestId = response.requestId,
            debugPath = "owli_backend.describe"
        )
    }

    private fun followUp(message: VlmChatMessage): VlmClientResult {
        val session = ensureSession()
        if (!session.canFollowUp) {
            throw IOException("Follow-up is not enabled for the current backend session.")
        }
        val sceneToken = activeSceneToken?.takeIf { !isSceneTokenExpired() }
            ?: throw IOException("No active backend scene is available. Start a new scene first.")
        val questionText = message.content.filterIsInstance<VlmContentPart.Text>()
            .joinToString(separator = "\n") { it.text.trim() }
            .trim()
        if (questionText.isBlank()) {
            throw IOException("Backend follow-up needs a text question.")
        }
        if (message.content.any { it is VlmContentPart.ImageUrl }) {
            throw IOException("Additional follow-up images are currently only supported in direct OpenRouter mode.")
        }
        val requestBody = JSONObject()
            .put("sessionToken", session.sessionToken)
            .put("installationId", installationIdProvider())
            .put("sceneToken", sceneToken)
            .put("questionText", questionText)

        val (body, _) = postJson("$baseUrl/api/v1/scene/followup", requestBody)
        val response = OwliBackendResponseParser.parseFollowUp(body)
            ?: throw IOException("Owli backend follow-up returned an invalid response.")
        return VlmClientResult(
            assistantContent = response.answerText,
            rawResponse = "<owli-backend-followup>",
            requestId = response.requestId,
            debugPath = "owli_backend.followup"
        )
    }

    private fun ensureSession(): OwliBackendBootstrapResponse {
        val current = activeSession
        if (current != null && !isSessionExpired(current)) {
            return current
        }
        val requestBody = JSONObject()
            .put("appVersion", appVersionProvider().trim())
            .put("versionCode", versionCodeProvider())
            .put("platform", "android")
            .put("installationId", installationIdProvider())
            .put("locale", localeProvider().ifBlank { "und" })
        val (body, _) = postJson("$baseUrl/api/v1/session/bootstrap", requestBody)
        return OwliBackendResponseParser.parseBootstrap(body)
            ?.also { activeSession = it }
            ?: throw IOException("Owli backend bootstrap returned an invalid response.")
    }

    private fun postJson(url: String, body: JSONObject): Pair<String, Int> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray(Charsets.UTF_8))
        }
        val httpCode = connection.responseCode
        val responseBody = (if (httpCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (httpCode !in 200..299) {
            val backendMessage = OwliBackendResponseParser.parseErrorMessage(responseBody)
            throw IOException(
                backendMessage ?: "Owli backend request failed with HTTP $httpCode."
            )
        }
        return responseBody to httpCode
    }

    private fun isSessionExpired(session: OwliBackendBootstrapResponse): Boolean {
        val expiresAt = session.expiresAt ?: return false
        return !expiresAt.isAfter(nowProvider().plusSeconds(30))
    }

    private fun isSceneTokenExpired(): Boolean {
        val expiresAt = activeSceneTokenExpiresAt ?: return false
        return !expiresAt.isAfter(nowProvider().plusSeconds(15))
    }

    private fun parseDataUrl(value: String): BackendImageDataUrl? {
        val match = DATA_URL_REGEX.matchEntire(value.trim()) ?: return null
        return BackendImageDataUrl(
            mimeType = match.groupValues[1],
            base64 = match.groupValues[2]
        )
    }

    private data class BackendImageDataUrl(
        val mimeType: String,
        val base64: String
    )

    private companion object {
        val DATA_URL_REGEX = Regex("^data:(image/(?:jpeg|png|webp));base64,(.+)$")
    }
}

class SwitchingVlmClient(
    private val backendClient: OwliBackendVlmClient,
    private val openRouterClient: OpenRouterVlmClient,
    initialSelection: VlmTransportSelection
) : VlmRuntimeClient {

    private var selection: VlmTransportSelection = initialSelection

    init {
        if (selection.activeMode != VlmTransportMode.BACKEND_MANAGED) {
            openRouterClient.updateApiKeySelection(selection.toOpenRouterApiKeySelection())
        }
    }

    override val transportMode: VlmTransportMode
        get() = selection.activeMode

    override val supportsFollowUpImageAttachments: Boolean
        get() = selection.activeMode != VlmTransportMode.BACKEND_MANAGED

    override val isConfigured: Boolean
        get() = when (selection.activeMode) {
            VlmTransportMode.BACKEND_MANAGED -> backendClient.isConfigured
            VlmTransportMode.DIRECT_OPENROUTER_BYOK,
            VlmTransportMode.EMBEDDED_DEBUG -> selection.hasUsableTransport && openRouterClient.isConfigured
        }

    override suspend fun chat(
        messages: List<VlmChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        return activeClient().chat(messages, maxTokens, temperature)
    }

    override suspend fun chatStreaming(
        messages: List<VlmChatMessage>,
        callback: VlmStreamingCallback,
        maxTokens: Int,
        temperature: Double
    ): VlmClientResult {
        return activeClient().chatStreaming(messages, callback, maxTokens, temperature)
    }

    override fun updateProfile(profile: VlmProfile) {
        backendClient.updateProfile(profile)
        openRouterClient.updateProfile(profile)
    }

    override fun updateTransportSelection(selection: VlmTransportSelection) {
        val previousMode = this.selection.activeMode
        this.selection = selection
        if (selection.activeMode != VlmTransportMode.BACKEND_MANAGED) {
            openRouterClient.updateApiKeySelection(selection.toOpenRouterApiKeySelection())
        }
        if (previousMode != selection.activeMode) {
            resetConversation()
        }
    }

    override fun resetConversation() {
        backendClient.resetConversation()
    }

    private fun activeClient(): VlmClient {
        return when (selection.activeMode) {
            VlmTransportMode.BACKEND_MANAGED -> backendClient
            VlmTransportMode.DIRECT_OPENROUTER_BYOK,
            VlmTransportMode.EMBEDDED_DEBUG -> openRouterClient
        }
    }
}
