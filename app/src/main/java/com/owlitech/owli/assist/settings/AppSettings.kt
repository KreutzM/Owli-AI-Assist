package com.owlitech.owli.assist.settings

data class AppSettings(
    val vlmProfileId: String = AppSettingsDefaults.vlmProfileId,
    val vlmProfileIdUserSet: Boolean = AppSettingsDefaults.vlmProfileIdUserSet,
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

object AppSettingsDefaults {
    const val vlmProfileId: String = "gpt4o_default"
    const val vlmProfileIdUserSet: Boolean = false
    val openRouterKeyMode: OpenRouterKeyMode = OpenRouterKeyMode.EMBEDDED_APP_KEY
    const val ttsEnabled: Boolean = true
    const val ttsSpeechRate: Float = 2.0f
    const val ttsPitch: Float = 1.0f
    const val streamingVlmTtsEnabled: Boolean = true
    val languagePreference: LanguagePreference = LanguagePreference.SYSTEM
}
