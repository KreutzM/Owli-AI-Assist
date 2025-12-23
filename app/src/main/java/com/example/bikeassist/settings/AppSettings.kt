package com.example.bikeassist.settings

import com.example.bikeassist.blindview.BlindViewConfig
import com.example.bikeassist.pipeline.AppMode
import com.example.bikeassist.ml.TfliteDetectorOptions

data class AppSettings(
    val appMode: AppMode = AppSettingsDefaults.appMode,
    val vlmProfileId: String = AppSettingsDefaults.vlmProfileId,
    val detectorMinConfidence: Float = AppSettingsDefaults.detectorMinConfidence,
    val detectorMaxResults: Int = AppSettingsDefaults.detectorMaxResults,
    val detectorNumThreads: Int = AppSettingsDefaults.detectorNumThreads,
    val detectorUseNnapi: Boolean = AppSettingsDefaults.detectorUseNnapi,
    val blindViewMinConfidence: Float = AppSettingsDefaults.blindViewMinConfidence,
    val minConfidenceTrack: Float = AppSettingsDefaults.minConfidenceTrack,
    val iouThreshold: Float = AppSettingsDefaults.iouThreshold,
    val bboxSmoothingAlpha: Float = AppSettingsDefaults.bboxSmoothingAlpha,
    val trackMaxAgeMs: Long = AppSettingsDefaults.trackMaxAgeMs,
    val minConsecutiveHits: Int = AppSettingsDefaults.minConsecutiveHits,
    val maxDetectionsPerFrameForTracking: Int = AppSettingsDefaults.maxDetectionsPerFrameForTracking,
    val minBboxAreaForTracking: Float = AppSettingsDefaults.minBboxAreaForTracking,
    val maxTracks: Int = AppSettingsDefaults.maxTracks,
    val nearThreshold: Float = AppSettingsDefaults.nearThreshold,
    val midThreshold: Float = AppSettingsDefaults.midThreshold,
    val maxItemsSpoken: Int = AppSettingsDefaults.maxItemsSpoken,
    val minSpeakIntervalMs: Long = AppSettingsDefaults.minSpeakIntervalMs,
    val repeatSamePlanIntervalMs: Long = AppSettingsDefaults.repeatSamePlanIntervalMs,
    val ttsSpeechRate: Float = AppSettingsDefaults.ttsSpeechRate,
    val showOverlay: Boolean = AppSettingsDefaults.showOverlay,
    val showBlindViewPreview: Boolean = AppSettingsDefaults.showBlindViewPreview,
    val showOverlayLabels: Boolean = AppSettingsDefaults.showOverlayLabels,
    val analysisIntervalMs: Long = AppSettingsDefaults.analysisIntervalMs
) {
    fun toDetectorOptions(): TfliteDetectorOptions {
        return TfliteDetectorOptions(
            modelPath = "models/efficientdet_lite2_int8.tflite",
            numThreads = detectorNumThreads.coerceIn(1, 4),
            useNnapi = detectorUseNnapi,
            maxResults = detectorMaxResults.coerceIn(1, 10),
            scoreThreshold = detectorMinConfidence.coerceIn(0.05f, 0.95f)
        )
    }

    fun toBlindViewConfig(): BlindViewConfig {
        return BlindViewConfig(
            minConfidence = blindViewMinConfidence.coerceIn(0.05f, 0.95f),
            nearThreshold = nearThreshold,
            midThreshold = midThreshold,
            maxItemsSpoken = maxItemsSpoken.coerceIn(1, 12),
            ttsSpeechRate = ttsSpeechRate.coerceIn(0.5f, 3.0f),
            minSpeakIntervalMs = minSpeakIntervalMs,
            repeatSamePlanIntervalMs = repeatSamePlanIntervalMs,
            iouThreshold = iouThreshold.coerceIn(0.1f, 0.9f),
            bboxSmoothingAlpha = bboxSmoothingAlpha.coerceIn(0f, 1f),
            trackMaxAgeMs = trackMaxAgeMs,
            minHitsToAnnounce = minConsecutiveHits.coerceIn(1, 5),
            minConfidenceTrack = minConfidenceTrack.coerceIn(0.05f, 0.95f),
            maxDetectionsPerFrameForTracking = maxDetectionsPerFrameForTracking.coerceIn(1, 30),
            minBboxAreaForTracking = minBboxAreaForTracking.coerceAtLeast(0f),
            minConsecutiveHitsToAnnounce = minConsecutiveHits.coerceIn(1, 5),
            confidenceEmaAlpha = bboxSmoothingAlpha.coerceIn(0f, 1f),
            confidenceDecayPerSecond = AppSettingsDefaults.confidenceDecayPerSecond,
            maxTracks = maxTracks.coerceIn(5, 100)
        )
    }
}

object AppSettingsDefaults {
    val appMode = AppMode.BLINDVIEW
    const val vlmProfileId: String = "safe"
    const val detectorMinConfidence: Float = 0.3f
    const val detectorMaxResults: Int = 3
    const val detectorNumThreads: Int = 2
    const val detectorUseNnapi: Boolean = false
    const val blindViewMinConfidence: Float = 0.3f
    const val minConfidenceTrack: Float = 0.45f
    const val iouThreshold: Float = 0.5f
    const val bboxSmoothingAlpha: Float = 0.4f
    const val trackMaxAgeMs: Long = 1_200L
    const val minConsecutiveHits: Int = 2
    const val maxDetectionsPerFrameForTracking: Int = 12
    const val minBboxAreaForTracking: Float = 0.0025f
    const val maxTracks: Int = 40
    const val nearThreshold: Float = 0.12f
    const val midThreshold: Float = 0.04f
    const val maxItemsSpoken: Int = 8
    const val minSpeakIntervalMs: Long = 2_500L
    const val repeatSamePlanIntervalMs: Long = 8_000L
    const val ttsSpeechRate: Float = 2.0f
    const val showOverlay: Boolean = true
    const val showBlindViewPreview: Boolean = true
    const val showOverlayLabels: Boolean = true
    const val analysisIntervalMs: Long = 250L
    const val confidenceDecayPerSecond: Float = 0.15f
}
