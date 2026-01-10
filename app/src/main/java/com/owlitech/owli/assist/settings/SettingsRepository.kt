package com.owlitech.owli.assist.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.owlitech.owli.assist.pipeline.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val updated = transform(current)
            prefs[PrefKeys.appMode] = updated.appMode.name
            prefs[PrefKeys.vlmProfileId] = updated.vlmProfileId
            prefs[PrefKeys.vlmProfileIdUserSet] = updated.vlmProfileIdUserSet
            prefs[PrefKeys.detectorMinConfidence] = updated.detectorMinConfidence
            prefs[PrefKeys.detectorMaxResults] = updated.detectorMaxResults
            prefs[PrefKeys.detectorNumThreads] = updated.detectorNumThreads
            prefs[PrefKeys.detectorUseNnapi] = updated.detectorUseNnapi
            prefs[PrefKeys.blindViewMinConfidence] = updated.blindViewMinConfidence
            prefs[PrefKeys.minConfidenceTrack] = updated.minConfidenceTrack
            prefs[PrefKeys.iouThreshold] = updated.iouThreshold
            prefs[PrefKeys.bboxSmoothingAlpha] = updated.bboxSmoothingAlpha
            prefs[PrefKeys.trackMaxAgeMs] = updated.trackMaxAgeMs
            prefs[PrefKeys.minConsecutiveHits] = updated.minConsecutiveHits
            prefs[PrefKeys.maxDetectionsPerFrameForTracking] = updated.maxDetectionsPerFrameForTracking
            prefs[PrefKeys.minBboxAreaForTracking] = updated.minBboxAreaForTracking
            prefs[PrefKeys.maxTracks] = updated.maxTracks
            prefs[PrefKeys.nearThreshold] = updated.nearThreshold
            prefs[PrefKeys.midThreshold] = updated.midThreshold
            prefs[PrefKeys.maxItemsSpoken] = updated.maxItemsSpoken
            prefs[PrefKeys.minSpeakIntervalMs] = updated.minSpeakIntervalMs
            prefs[PrefKeys.repeatSamePlanIntervalMs] = updated.repeatSamePlanIntervalMs
            prefs[PrefKeys.ttsSpeechRate] = updated.ttsSpeechRate
            prefs[PrefKeys.ttsPitch] = updated.ttsPitch
            prefs[PrefKeys.streamingVlmTtsEnabled] = updated.streamingVlmTtsEnabled
            prefs[PrefKeys.showOverlay] = updated.showOverlay
            prefs[PrefKeys.showBlindViewPreview] = updated.showBlindViewPreview
            prefs[PrefKeys.showOverlayLabels] = updated.showOverlayLabels
            prefs[PrefKeys.analysisIntervalMs] = updated.analysisIntervalMs
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
    val appMode = stringPreferencesKey("appMode")
    val vlmProfileId = stringPreferencesKey("vlmProfileId")
    val vlmProfileIdUserSet = booleanPreferencesKey("vlmProfileIdUserSet")
    val detectorMinConfidence = floatPreferencesKey("detectorMinConfidence")
    val detectorMaxResults = intPreferencesKey("detectorMaxResults")
    val detectorNumThreads = intPreferencesKey("detectorNumThreads")
    val detectorUseNnapi = booleanPreferencesKey("detectorUseNnapi")
    val blindViewMinConfidence = floatPreferencesKey("blindViewMinConfidence")
    val minConfidenceTrack = floatPreferencesKey("minConfidenceTrack")
    val iouThreshold = floatPreferencesKey("iouThreshold")
    val bboxSmoothingAlpha = floatPreferencesKey("bboxSmoothingAlpha")
    val trackMaxAgeMs = longPreferencesKey("trackMaxAgeMs")
    val minConsecutiveHits = intPreferencesKey("minConsecutiveHits")
    val maxDetectionsPerFrameForTracking = intPreferencesKey("maxDetectionsPerFrameForTracking")
    val minBboxAreaForTracking = floatPreferencesKey("minBboxAreaForTracking")
    val maxTracks = intPreferencesKey("maxTracks")
    val nearThreshold = floatPreferencesKey("nearThreshold")
    val midThreshold = floatPreferencesKey("midThreshold")
    val maxItemsSpoken = intPreferencesKey("maxItemsSpoken")
    val minSpeakIntervalMs = longPreferencesKey("minSpeakIntervalMs")
    val repeatSamePlanIntervalMs = longPreferencesKey("repeatSamePlanIntervalMs")
    val ttsSpeechRate = floatPreferencesKey("ttsSpeechRate")
    val ttsPitch = floatPreferencesKey("ttsPitch")
    val streamingVlmTtsEnabled = booleanPreferencesKey("streamingVlmTtsEnabled")
    val showOverlay = booleanPreferencesKey("showOverlay")
    val showBlindViewPreview = booleanPreferencesKey("showBlindViewPreview")
    val showOverlayLabels = booleanPreferencesKey("showOverlayLabels")
    val analysisIntervalMs = longPreferencesKey("analysisIntervalMs")
    val languagePreference = stringPreferencesKey("languagePreference")
}

private fun androidx.datastore.preferences.core.Preferences.toSettings(): AppSettings {
    val storedProfileId = this[PrefKeys.vlmProfileId]
    val profileIdUserSet = this[PrefKeys.vlmProfileIdUserSet] ?: (storedProfileId != null)
    return AppSettings(
        appMode = AppMode.valueOf(this[PrefKeys.appMode] ?: AppSettingsDefaults.appMode.name),
        vlmProfileId = storedProfileId ?: AppSettingsDefaults.vlmProfileId,
        vlmProfileIdUserSet = profileIdUserSet,
        detectorMinConfidence = this[PrefKeys.detectorMinConfidence] ?: AppSettingsDefaults.detectorMinConfidence,
        detectorMaxResults = this[PrefKeys.detectorMaxResults] ?: AppSettingsDefaults.detectorMaxResults,
        detectorNumThreads = this[PrefKeys.detectorNumThreads] ?: AppSettingsDefaults.detectorNumThreads,
        detectorUseNnapi = this[PrefKeys.detectorUseNnapi] ?: AppSettingsDefaults.detectorUseNnapi,
        blindViewMinConfidence = this[PrefKeys.blindViewMinConfidence] ?: AppSettingsDefaults.blindViewMinConfidence,
        minConfidenceTrack = this[PrefKeys.minConfidenceTrack] ?: AppSettingsDefaults.minConfidenceTrack,
        iouThreshold = this[PrefKeys.iouThreshold] ?: AppSettingsDefaults.iouThreshold,
        bboxSmoothingAlpha = this[PrefKeys.bboxSmoothingAlpha] ?: AppSettingsDefaults.bboxSmoothingAlpha,
        trackMaxAgeMs = this[PrefKeys.trackMaxAgeMs] ?: AppSettingsDefaults.trackMaxAgeMs,
        minConsecutiveHits = this[PrefKeys.minConsecutiveHits] ?: AppSettingsDefaults.minConsecutiveHits,
        maxDetectionsPerFrameForTracking = this[PrefKeys.maxDetectionsPerFrameForTracking]
            ?: AppSettingsDefaults.maxDetectionsPerFrameForTracking,
        minBboxAreaForTracking = this[PrefKeys.minBboxAreaForTracking] ?: AppSettingsDefaults.minBboxAreaForTracking,
        maxTracks = this[PrefKeys.maxTracks] ?: AppSettingsDefaults.maxTracks,
        nearThreshold = this[PrefKeys.nearThreshold] ?: AppSettingsDefaults.nearThreshold,
        midThreshold = this[PrefKeys.midThreshold] ?: AppSettingsDefaults.midThreshold,
        maxItemsSpoken = this[PrefKeys.maxItemsSpoken] ?: AppSettingsDefaults.maxItemsSpoken,
        minSpeakIntervalMs = this[PrefKeys.minSpeakIntervalMs] ?: AppSettingsDefaults.minSpeakIntervalMs,
        repeatSamePlanIntervalMs = this[PrefKeys.repeatSamePlanIntervalMs] ?: AppSettingsDefaults.repeatSamePlanIntervalMs,
        ttsSpeechRate = this[PrefKeys.ttsSpeechRate] ?: AppSettingsDefaults.ttsSpeechRate,
        ttsPitch = this[PrefKeys.ttsPitch] ?: AppSettingsDefaults.ttsPitch,
        streamingVlmTtsEnabled = this[PrefKeys.streamingVlmTtsEnabled] ?: AppSettingsDefaults.streamingVlmTtsEnabled,
        showOverlay = this[PrefKeys.showOverlay] ?: AppSettingsDefaults.showOverlay,
        showBlindViewPreview = this[PrefKeys.showBlindViewPreview] ?: AppSettingsDefaults.showBlindViewPreview,
        showOverlayLabels = this[PrefKeys.showOverlayLabels] ?: AppSettingsDefaults.showOverlayLabels,
        analysisIntervalMs = this[PrefKeys.analysisIntervalMs] ?: AppSettingsDefaults.analysisIntervalMs,
        languagePreference = LanguagePreference.valueOf(
            this[PrefKeys.languagePreference] ?: AppSettingsDefaults.languagePreference.name
        )
    )
}
