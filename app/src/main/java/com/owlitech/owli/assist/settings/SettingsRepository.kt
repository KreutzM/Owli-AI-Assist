package com.owlitech.owli.assist.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val updated = transform(current)
            prefs[PrefKeys.vlmProfileId] = updated.vlmProfileId
            prefs[PrefKeys.vlmProfileIdUserSet] = updated.vlmProfileIdUserSet
            prefs[PrefKeys.openRouterKeyMode] = updated.openRouterKeyMode.name
            prefs[PrefKeys.ttsEnabled] = updated.ttsEnabled
            prefs[PrefKeys.ttsSpeechRate] = updated.ttsSpeechRate
            prefs[PrefKeys.ttsPitch] = updated.ttsPitch
            prefs[PrefKeys.streamingVlmTtsEnabled] = updated.streamingVlmTtsEnabled
            prefs[PrefKeys.languagePreference] = updated.languagePreference.name
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

private object PrefKeys {
    val vlmProfileId = stringPreferencesKey("vlmProfileId")
    val vlmProfileIdUserSet = booleanPreferencesKey("vlmProfileIdUserSet")
    val openRouterKeyMode = stringPreferencesKey("openRouterKeyMode")
    val ttsEnabled = booleanPreferencesKey("ttsEnabled")
    val ttsSpeechRate = floatPreferencesKey("ttsSpeechRate")
    val ttsPitch = floatPreferencesKey("ttsPitch")
    val streamingVlmTtsEnabled = booleanPreferencesKey("streamingVlmTtsEnabled")
    val languagePreference = stringPreferencesKey("languagePreference")
}

private fun androidx.datastore.preferences.core.Preferences.toSettings(): AppSettings {
    val storedProfileId = this[PrefKeys.vlmProfileId]
    val profileIdUserSet = this[PrefKeys.vlmProfileIdUserSet] ?: (storedProfileId != null)
    return AppSettings(
        vlmProfileId = storedProfileId ?: AppSettingsDefaults.vlmProfileId,
        vlmProfileIdUserSet = profileIdUserSet,
        openRouterKeyMode = enumValueOrDefault(
            this[PrefKeys.openRouterKeyMode],
            AppSettingsDefaults.openRouterKeyMode
        ),
        ttsEnabled = this[PrefKeys.ttsEnabled] ?: AppSettingsDefaults.ttsEnabled,
        ttsSpeechRate = this[PrefKeys.ttsSpeechRate] ?: AppSettingsDefaults.ttsSpeechRate,
        ttsPitch = this[PrefKeys.ttsPitch] ?: AppSettingsDefaults.ttsPitch,
        streamingVlmTtsEnabled = this[PrefKeys.streamingVlmTtsEnabled]
            ?: AppSettingsDefaults.streamingVlmTtsEnabled,
        languagePreference = enumValueOrDefault(
            this[PrefKeys.languagePreference],
            AppSettingsDefaults.languagePreference
        )
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
    return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
