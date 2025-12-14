package com.example.bikeassist.blindview

data class BlindViewConfig(
    val minConfidence: Float = 0.3f,
    val nearThreshold: Float = 0.12f,
    val midThreshold: Float = 0.04f,
    val minSpeakIntervalMs: Long = 2_500L,
    val repeatSamePlanIntervalMs: Long = 8_000L,
    val maxItemsSpoken: Int = 8,
    val ttsSpeechRate: Float = 2.0f
)
