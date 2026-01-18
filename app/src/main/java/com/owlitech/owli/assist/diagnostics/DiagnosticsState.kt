package com.owlitech.owli.assist.diagnostics

import com.owlitech.owli.assist.motion.MotionLevel

data class DiagnosticsState(
    val versionName: String = "unknown",
    val versionCode: Int = 0,
    val buildType: String = "debug",
    val deviceModel: String = android.os.Build.MODEL ?: "unknown",
    val androidVersion: String = android.os.Build.VERSION.RELEASE ?: "unknown",
    val isRunning: Boolean = false,
    val lastFrameAt: Long = 0L,
    val frameIntervalMs: Float = 0f,
    val fps: Float = 0f,
    val detectorInfo: String = "",
    val detectorNumThreads: Int = 0,
    val detectorScoreThreshold: Float = 0f,
    val detectorMaxResults: Int = 0,
    val analysisIntervalMs: Long = 0L,
    val blindViewMinConfidence: Float = 0f,
    val ttsReady: Boolean = false,
    val ttsSpeechRate: Float = 1.0f,
    val minSpeakIntervalMs: Long = 2_500L,
    val repeatSamePlanIntervalMs: Long = 8_000L,
    val maxItemsSpoken: Int = 8,
    val iouThreshold: Float = 0.5f,
    val trackMaxAgeMs: Long = 1_200L,
    val minConsecutiveHits: Int = 2,
    val detectionsCountRaw: Int = 0,
    val detectionsCountStable: Int = 0,
    val topLabels: List<String> = emptyList(),
    val lastUtterancePreview: String? = null,
    val showOverlay: Boolean = true,
    val showOverlayLabels: Boolean = true,
    val showBlindViewPreview: Boolean = true,
    val motionLevel: MotionLevel? = null,
    val gyroMagRadS: Float = 0f,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val motionQuality: Float = 0f,
    val stabilizationEnabled: Boolean = false,
    val appliedRollDeg: Float = 0f,
    val mappingActive: Boolean = false
)

data class TtsDiagnostics(
    val ready: Boolean,
    val speechRate: Float
)
