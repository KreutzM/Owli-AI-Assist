package com.owlitech.owli.assist.diagnostics

import com.owlitech.owli.assist.settings.AppSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsReportBuilder {
    fun build(state: DiagnosticsState, settings: AppSettings): String {
        val sb = StringBuilder()
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("=== Owli-AI Diagnostics ===")
        sb.appendLine("Time: ${df.format(Date())}")
        sb.appendLine()
        sb.appendLine("App:")
        sb.appendLine("  version: ${state.versionName} (${state.versionCode}) build=${state.buildType}")
        sb.appendLine("  device: ${state.deviceModel} android=${state.androidVersion}")
        sb.appendLine()
        sb.appendLine("Pipeline:")
        sb.appendLine("  running: ${state.isRunning} fps=${"%.2f".format(state.fps)} intervalMs=${"%.1f".format(state.frameIntervalMs)}")
        sb.appendLine("  detectorInfo: ${state.detectorInfo}")
        sb.appendLine("  analysisIntervalMs: ${state.analysisIntervalMs}")
        sb.appendLine("  debugDetectorViewEnabled: ${state.debugDetectorViewEnabled}")
        sb.appendLine()
        sb.appendLine("Detector:")
        sb.appendLine("  threads=${state.detectorNumThreads} score>=${state.detectorScoreThreshold} maxResults=${state.detectorMaxResults}")
        sb.appendLine()
        sb.appendLine("BlindView/TTS:")
        sb.appendLine("  ttsReady=${state.ttsReady} speechRate=${"%.2f".format(state.ttsSpeechRate)}")
        sb.appendLine("  minSpeakIntervalMs=${state.minSpeakIntervalMs} repeatSamePlanIntervalMs=${state.repeatSamePlanIntervalMs}")
        sb.appendLine("  maxItemsSpoken=${state.maxItemsSpoken}")
        sb.appendLine()
        sb.appendLine("Tracking:")
        sb.appendLine("  iouThreshold=${state.iouThreshold} trackMaxAgeMs=${state.trackMaxAgeMs} minConsecutiveHits=${state.minConsecutiveHits}")
        sb.appendLine("  showOverlay=${state.showOverlay} showLabels=${state.showOverlayLabels} showPreview=${state.showBlindViewPreview}")
        sb.appendLine()
        sb.appendLine("Motion:")
        sb.appendLine(
            "  level=${state.motionLevel ?: "-"} gyro=${"%.2f".format(state.gyroMagRadS)} roll=${"%.1f".format(state.rollDeg)} pitch=${"%.1f".format(state.pitchDeg)} quality=${"%.2f".format(state.motionQuality)}"
        )
        sb.appendLine(
            "  stabilizationEnabled=${state.stabilizationEnabled} appliedRollDeg=${"%.1f".format(state.appliedRollDeg)} mappingActive=${state.mappingActive}"
        )
        sb.appendLine()
        sb.appendLine("Scene Snapshot:")
        sb.appendLine("  detectionsRaw=${state.detectionsCountRaw} detectionsStable=${state.detectionsCountStable}")
        sb.appendLine("  topLabels=${state.topLabels.joinToString()}")
        sb.appendLine("  lastPreview=${state.lastUtterancePreview ?: "-"}")
        sb.appendLine()
        sb.appendLine("Settings (current):")
        sb.appendLine("  appMode=${settings.appMode}")
        sb.appendLine("  detector: conf=${settings.detectorMinConfidence} maxResults=${settings.detectorMaxResults} threads=${settings.detectorNumThreads} nnapi=${settings.detectorUseNnapi}")
        sb.appendLine("  blindView: minConfidence=${settings.blindViewMinConfidence} minConfidenceTrack=${settings.minConfidenceTrack} iou=${settings.iouThreshold}")
        sb.appendLine("             bboxAlpha=${settings.bboxSmoothingAlpha} minHits=${settings.minConsecutiveHits} maxDetections=${settings.maxDetectionsPerFrameForTracking} maxTracks=${settings.maxTracks}")
        sb.appendLine("  ttsSpeechRate=${settings.ttsSpeechRate} ttsPitch=${settings.ttsPitch}")
        sb.appendLine("  streamingVlmTtsEnabled=${settings.streamingVlmTtsEnabled}")
        sb.appendLine(
            "  motionGating=${settings.enableMotionGating} motionMed=${settings.motionMedThresholdRadS} motionHigh=${settings.motionHighThresholdRadS} motionSpeakMultiplierHigh=${settings.motionSpeakIntervalMultiplierHigh}"
        )
        sb.appendLine(
            "  imuDerotation=${settings.enableImuDerotation} stabilizationQualityMin=${settings.stabilizationQualityMin}"
        )
        return sb.toString()
    }
}
