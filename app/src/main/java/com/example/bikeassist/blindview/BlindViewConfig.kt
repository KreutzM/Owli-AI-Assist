package com.example.bikeassist.blindview

data class BlindViewConfig(
    val minConfidence: Float = 0.3f,
    val nearThreshold: Float = 0.12f,
    val midThreshold: Float = 0.04f,
    val minSpeakIntervalMs: Long = 2_500L,
    val repeatSamePlanIntervalMs: Long = 8_000L,
    val maxItemsSpoken: Int = 8,
    val ttsSpeechRate: Float = 2.0f,
    val iouThreshold: Float = 0.5f,
    val bboxSmoothingAlpha: Float = 0.4f,
    val trackMaxAgeMs: Long = 1_200L,
    val minHitsToAnnounce: Int = 2,
    val resetAfterGapMs: Long = 3_000L,
    val minConfidenceTrack: Float = 0.45f,
    val maxDetectionsPerFrameForTracking: Int = 12,
    val minBboxAreaForTracking: Float = 0.0025f,
    val minConsecutiveHitsToAnnounce: Int = 2,
    val confidenceEmaAlpha: Float = 0.5f,
    val confidenceDecayPerSecond: Float = 0.15f,
    val maxTracks: Int = 40
)
