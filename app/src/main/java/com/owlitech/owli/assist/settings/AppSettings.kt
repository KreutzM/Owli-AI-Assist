package com.owlitech.owli.assist.settings

data class AppSettings(
    val vlmProfileId: String = AppSettingsDefaults.vlmProfileId,
    val vlmProfileIdUserSet: Boolean = AppSettingsDefaults.vlmProfileIdUserSet,
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

object AppSettingsDefaults {
    const val vlmProfileId: String = "gpt4o_default"
    const val vlmProfileIdUserSet: Boolean = false
    const val ttsEnabled: Boolean = true
    const val ttsSpeechRate: Float = 2.0f
    const val ttsPitch: Float = 1.0f
    const val streamingVlmTtsEnabled: Boolean = true
    val languagePreference: LanguagePreference = LanguagePreference.SYSTEM
}
