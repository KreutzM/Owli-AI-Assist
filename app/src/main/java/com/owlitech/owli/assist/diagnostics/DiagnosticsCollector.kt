package com.owlitech.owli.assist.diagnostics

import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.motion.MotionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI

object DiagnosticsCollector {
    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state

    private val lastFrameAt = AtomicLong(0L)

    fun updatePipelineStatus(
        isRunning: Boolean,
        detectorInfo: String,
        analysisIntervalMs: Long
    ) {
        _state.value = _state.value.copy(
            isRunning = isRunning,
            detectorInfo = detectorInfo,
            analysisIntervalMs = analysisIntervalMs
        )
    }

    fun setAppInfo(versionName: String, versionCode: Int, buildType: String) {
        _state.value = _state.value.copy(
            versionName = versionName,
            versionCode = versionCode,
            buildType = buildType
        )
    }

    fun updateSettings(settings: AppSettings) {
        _state.value = _state.value.copy(
            detectorScoreThreshold = settings.detectorMinConfidence,
            detectorMaxResults = settings.detectorMaxResults,
            detectorNumThreads = settings.detectorNumThreads,
            blindViewMinConfidence = settings.blindViewMinConfidence,
            minSpeakIntervalMs = settings.minSpeakIntervalMs,
            repeatSamePlanIntervalMs = settings.repeatSamePlanIntervalMs,
            maxItemsSpoken = settings.maxItemsSpoken,
            iouThreshold = settings.iouThreshold,
            trackMaxAgeMs = settings.trackMaxAgeMs,
            minConsecutiveHits = settings.minConsecutiveHits,
            showOverlay = settings.showOverlay,
            showOverlayLabels = settings.showOverlayLabels,
            showBlindViewPreview = settings.showBlindViewPreview,
            analysisIntervalMs = settings.analysisIntervalMs,
            stabilizationEnabled = settings.enableImuDerotation,
            debugDetectorViewEnabled = settings.enableDetectorDebugView
        )
    }

    fun updateFrameProcessed(timestampMs: Long) {
        val prev = lastFrameAt.getAndSet(timestampMs)
        if (prev > 0) {
            val delta = (timestampMs - prev).coerceAtLeast(1)
            val prevInterval = _state.value.frameIntervalMs
            val newInterval = if (prevInterval == 0f) delta.toFloat() else prevInterval * 0.8f + delta * 0.2f
            val fps = if (newInterval > 0f) 1000f / newInterval else 0f
            _state.value = _state.value.copy(
                lastFrameAt = timestampMs,
                frameIntervalMs = newInterval,
                fps = fps
            )
        } else {
            _state.value = _state.value.copy(lastFrameAt = timestampMs)
        }
    }

    fun updateTts(ttsReady: Boolean, speechRate: Float) {
        _state.value = _state.value.copy(
            ttsReady = ttsReady,
            ttsSpeechRate = speechRate
        )
    }

    fun updateSceneSnapshot(detections: Int, topLabels: List<String>, preview: String?) {
        _state.value = _state.value.copy(
            detectionsCountRaw = detections,
            detectionsCountStable = detections,
            topLabels = topLabels.take(5),
            lastUtterancePreview = preview
        )
    }

    fun updateMotion(snapshot: MotionSnapshot?) {
        if (snapshot == null) {
            _state.value = _state.value.copy(
                motionLevel = null,
                gyroMagRadS = 0f,
                rollDeg = 0f,
                pitchDeg = 0f,
                motionQuality = 0f
            )
            return
        }
        _state.value = _state.value.copy(
            motionLevel = snapshot.motionLevel,
            gyroMagRadS = snapshot.gyroMagRadS,
            rollDeg = radToDeg(snapshot.rollRad),
            pitchDeg = radToDeg(snapshot.pitchRad),
            motionQuality = snapshot.quality
        )
    }

    fun updateStabilization(appliedRollDeg: Float, mappingActive: Boolean) {
        _state.value = _state.value.copy(
            appliedRollDeg = appliedRollDeg,
            mappingActive = mappingActive
        )
    }

    fun updateTranslation(
        dxLowRes: Int,
        dyLowRes: Int,
        quality: Float,
        cropLeftPx: Int,
        cropTopPx: Int
    ) {
        _state.value = _state.value.copy(
            translationDxLowRes = dxLowRes,
            translationDyLowRes = dyLowRes,
            translationQuality = quality,
            cropLeftPx = cropLeftPx,
            cropTopPx = cropTopPx
        )
    }

    private fun radToDeg(rad: Float): Float {
        return (rad * 180f / PI.toFloat())
    }
}
