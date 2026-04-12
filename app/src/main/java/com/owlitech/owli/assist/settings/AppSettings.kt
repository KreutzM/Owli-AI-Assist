package com.owlitech.owli.assist.settings

data class AppSettings(
    val vlmProfileId: String = AppSettingsDefaults.vlmProfileId,
    val vlmProfileIdUserSet: Boolean = AppSettingsDefaults.vlmProfileIdUserSet,
    val vlmTransportMode: VlmTransportMode = AppSettingsDefaults.vlmTransportMode,
    val openRouterKeyMode: OpenRouterKeyMode = AppSettingsDefaults.openRouterKeyMode,
    val ttsEnabled: Boolean = AppSettingsDefaults.ttsEnabled,
    val ttsSpeechRate: Float = AppSettingsDefaults.ttsSpeechRate,
    val ttsPitch: Float = AppSettingsDefaults.ttsPitch,
    val streamingVlmTtsEnabled: Boolean = AppSettingsDefaults.streamingVlmTtsEnabled,
    val languagePreference: LanguagePreference = AppSettingsDefaults.languagePreference
)

enum class LanguagePreference {
    SYSTEM,
    DE,
    EN
}

enum class VlmTransportMode {
    BACKEND_MANAGED,
    DIRECT_OPENROUTER_BYOK,
    EMBEDDED_DEBUG
}

enum class OpenRouterKeyMode {
    EMBEDDED_APP_KEY,
    USER_PROVIDED_KEY
}

data class OpenRouterApiKeySelection(
    val requestedMode: OpenRouterKeyMode,
    val activeMode: OpenRouterKeyMode,
    val apiKey: String
) {
    val hasUsableKey: Boolean = apiKey.isNotBlank()
}

data class VlmTransportSelection(
    val requestedMode: VlmTransportMode,
    val activeMode: VlmTransportMode,
    val apiKey: String? = null
) {
    val hasUsableTransport: Boolean = when (activeMode) {
        VlmTransportMode.BACKEND_MANAGED -> true
        VlmTransportMode.DIRECT_OPENROUTER_BYOK,
        VlmTransportMode.EMBEDDED_DEBUG -> !apiKey.isNullOrBlank()
    }
}

fun resolveOpenRouterApiKeySelection(
    settings: AppSettings,
    embeddedAppKey: String,
    userProvidedKey: String? = null
): OpenRouterApiKeySelection {
    val trimmedUserKey = userProvidedKey?.trim().orEmpty()
    if (settings.openRouterKeyMode == OpenRouterKeyMode.USER_PROVIDED_KEY && trimmedUserKey.isNotBlank()) {
        return OpenRouterApiKeySelection(
            requestedMode = settings.openRouterKeyMode,
            activeMode = OpenRouterKeyMode.USER_PROVIDED_KEY,
            apiKey = trimmedUserKey
        )
    }
    return OpenRouterApiKeySelection(
        requestedMode = settings.openRouterKeyMode,
        activeMode = OpenRouterKeyMode.EMBEDDED_APP_KEY,
        apiKey = embeddedAppKey.trim()
    )
}

fun resolveVlmTransportSelection(
    settings: AppSettings,
    embeddedAppKey: String,
    userProvidedKey: String? = null
): VlmTransportSelection {
    val trimmedUserKey = userProvidedKey?.trim().orEmpty()
    val trimmedEmbeddedKey = embeddedAppKey.trim()
    return when (settings.vlmTransportMode) {
        VlmTransportMode.BACKEND_MANAGED -> VlmTransportSelection(
            requestedMode = settings.vlmTransportMode,
            activeMode = VlmTransportMode.BACKEND_MANAGED
        )
        VlmTransportMode.DIRECT_OPENROUTER_BYOK -> VlmTransportSelection(
            requestedMode = settings.vlmTransportMode,
            activeMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK,
            apiKey = trimmedUserKey
        )
        VlmTransportMode.EMBEDDED_DEBUG -> VlmTransportSelection(
            requestedMode = settings.vlmTransportMode,
            activeMode = VlmTransportMode.EMBEDDED_DEBUG,
            apiKey = trimmedEmbeddedKey
        )
    }
}

fun VlmTransportSelection.toOpenRouterApiKeySelection(): OpenRouterApiKeySelection {
    return when (activeMode) {
        VlmTransportMode.BACKEND_MANAGED -> error("Backend transport does not use a direct OpenRouter key.")
        VlmTransportMode.DIRECT_OPENROUTER_BYOK -> OpenRouterApiKeySelection(
            requestedMode = OpenRouterKeyMode.USER_PROVIDED_KEY,
            activeMode = OpenRouterKeyMode.USER_PROVIDED_KEY,
            apiKey = apiKey.orEmpty()
        )
        VlmTransportMode.EMBEDDED_DEBUG -> OpenRouterApiKeySelection(
            requestedMode = OpenRouterKeyMode.EMBEDDED_APP_KEY,
            activeMode = OpenRouterKeyMode.EMBEDDED_APP_KEY,
            apiKey = apiKey.orEmpty()
        )
    }
}

object AppSettingsDefaults {
    const val vlmProfileId: String = "gpt4o_default"
    const val vlmProfileIdUserSet: Boolean = false
    val vlmTransportMode: VlmTransportMode = VlmTransportMode.BACKEND_MANAGED
    val openRouterKeyMode: OpenRouterKeyMode = OpenRouterKeyMode.EMBEDDED_APP_KEY
    const val ttsEnabled: Boolean = true
    const val ttsSpeechRate: Float = 2.0f
    const val ttsPitch: Float = 1.0f
    const val streamingVlmTtsEnabled: Boolean = true
    val languagePreference: LanguagePreference = LanguagePreference.SYSTEM
}
