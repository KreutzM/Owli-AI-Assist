package com.owlitech.owli.assist.processing

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.scale
import com.owlitech.owli.assist.domain.TrafficLightObservation
import com.owlitech.owli.assist.domain.TrafficLightPhase
import com.owlitech.owli.assist.ml.BoundingBox
import com.owlitech.owli.assist.ml.Detection
import kotlin.math.max

interface TrafficLightPhaseClassifier {
    fun classify(frame: Bitmap, detections: List<Detection>): List<TrafficLightObservation>
}

class HsvTrafficLightPhaseClassifier(
    private val saturationThresholdLoose: Float = 0.25f,
    private val valueThresholdLoose: Float = 0.25f,
    private val saturationThresholdStrict: Float = 0.45f,
    private val valueThresholdStrict: Float = 0.45f,
    private val minScore: Float = 0.06f,
    private val dominanceRatio: Float = 1.4f,
    private val insetFraction: Float = 0.15f,
    private val minRoiSizePx: Int = 24,
    private val roiSize: Int = 72,
    private val logInterval: Int = 60
) : TrafficLightPhaseClassifier {

    private var frameCount = 0
    private var stablePhase: TrafficLightPhase = TrafficLightPhase.UNKNOWN
    private var stablePhaseCount: Int = 0
    private var lastCandidate: TrafficLightPhase = TrafficLightPhase.UNKNOWN
    private var candidateCount: Int = 0

    override fun classify(frame: Bitmap, detections: List<Detection>): List<TrafficLightObservation> {
        frameCount++
        val trafficDetections = detections.filter { it.label.equals("traffic light", ignoreCase = true) && it.confidence >= 0.3f }
        if (trafficDetections.isEmpty()) {
            decayUnknown()
            return emptyList()
        }

        val primary = selectPrimary(trafficDetections) ?: run {
            decayUnknown()
            return emptyList()
        }

        val roi = innerCrop(frame, primary.bbox) ?: run {
            decayUnknown()
            return emptyList()
        }
        if (roi.width < minRoiSizePx || roi.height < minRoiSizePx) {
            decayUnknown()
            return emptyList()
        }

        val scaled = roi.scale(roiSize, roiSize, true)
        val (phase, conf, redTop, greenBottom) = scorePhaseZoned(scaled)
        val observation = TrafficLightObservation(
            bbox = primary.bbox,
            phase = phase,
            confidence = conf,
            redTop = redTop,
            greenBottom = greenBottom
        )

        updateStable(phase, conf)

        if (frameCount % logInterval == 0) {
            Log.d(TAG, "TL primary=${phase} conf=${"%.2f".format(conf)} redTop=${"%.3f".format(redTop)} greenBottom=${"%.3f".format(greenBottom)} stable=$stablePhase cCount=$candidateCount sCount=$stablePhaseCount roi=${scaled.width}x${scaled.height}")
        }

        return listOf(observation.copy(phase = stablePhase))
    }

    private fun selectPrimary(detections: List<Detection>): Detection? {
        return detections.maxByOrNull { det ->
            val area = (det.bbox.xMax - det.bbox.xMin) * (det.bbox.yMax - det.bbox.yMin)
            val heightBias = 1f - det.bbox.yMin * 0.1f // leicht bevorzugt oben hängende Ampeln
            area * heightBias + det.confidence * 0.01f
        }
    }

    private fun innerCrop(frame: Bitmap, bbox: BoundingBox): Bitmap? {
        val insetX = (bbox.xMax - bbox.xMin) * insetFraction
        val insetY = (bbox.yMax - bbox.yMin) * insetFraction
        val xMin = (bbox.xMin + insetX).coerceIn(0f, 1f)
        val yMin = (bbox.yMin + insetY).coerceIn(0f, 1f)
        val xMax = (bbox.xMax - insetX).coerceIn(0f, 1f)
        val yMax = (bbox.yMax - insetY).coerceIn(0f, 1f)
        val left = (xMin * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (yMin * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (xMax * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (yMax * frame.height).toInt().coerceIn(top + 1, frame.height)
        val width = right - left
        val height = bottom - top
        if (width < 2 || height < 2) return null
        return Bitmap.createBitmap(frame, left, top, width, height)
    }

    private fun scorePhaseZoned(bitmap: Bitmap): Quad {
        val w = bitmap.width
        val h = bitmap.height
        val hsv = FloatArray(3)

        var redTopStrict = 0
        var redTopLoose = 0
        var validTop = 0

        var greenBottomStrict = 0
        var greenBottomLoose = 0
        var validBottom = 0

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val topEnd = (h * 0.33f).toInt()
        val bottomStart = (h * 0.66f).toInt()

        for (y in 0 until h) {
            val rowStart = y * w
            for (x in 0 until w) {
                val p = pixels[rowStart + x]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                val hue = hsv[0]

                val inTop = y < topEnd
                val inBottom = y >= bottomStart

                if (inTop) {
                    if (sat >= saturationThresholdLoose && value >= valueThresholdLoose) {
                        validTop++
                        val isRedLoose = (hue <= 15f || hue >= 345f)
                        if (isRedLoose) redTopLoose++
                        if (sat >= saturationThresholdStrict && value >= valueThresholdStrict && isRedLoose) {
                            redTopStrict++
                        }
                    }
                } else if (inBottom) {
                    if (sat >= saturationThresholdLoose && value >= valueThresholdLoose) {
                        validBottom++
                        val isGreenLoose = (hue in 80f..160f)
                        if (isGreenLoose) greenBottomLoose++
                        if (sat >= saturationThresholdStrict && value >= valueThresholdStrict && isGreenLoose) {
                            greenBottomStrict++
                        }
                    }
                }
            }
        }

        val redScore = computeScore(redTopStrict, redTopLoose, validTop)
        val greenScore = computeScore(greenBottomStrict, greenBottomLoose, validBottom)

        val phase = when {
            redScore > minScore && redScore > greenScore * dominanceRatio -> TrafficLightPhase.RED
            greenScore > minScore && greenScore > redScore * dominanceRatio -> TrafficLightPhase.GREEN
            else -> TrafficLightPhase.UNKNOWN
        }
        val conf = max(redScore, greenScore).coerceIn(0f, 1f)
        return Quad(phase, conf, redScore, greenScore)
    }

    private fun computeScore(strictCount: Int, looseCount: Int, valid: Int): Float {
        if (valid == 0) return 0f
        return if (strictCount > 0) strictCount.toFloat() / valid else looseCount.toFloat() / valid
    }

    private fun updateStable(candidate: TrafficLightPhase, candidateConf: Float) {
        if (candidate == stablePhase) {
            stablePhaseCount = (stablePhaseCount + 1).coerceAtMost(10)
            candidateCount = 0
            lastCandidate = candidate
            return
        }
        if (candidate == TrafficLightPhase.UNKNOWN) {
            stablePhaseCount = (stablePhaseCount - 1).coerceAtLeast(0)
            if (stablePhaseCount == 0) {
                stablePhase = TrafficLightPhase.UNKNOWN
            }
            return
        }
        if (candidate == lastCandidate) {
            candidateCount++
        } else {
            candidateCount = 1
            lastCandidate = candidate
        }
        val highConf = candidateConf > 0.14f
        if (candidateCount >= 3 || (highConf && candidateCount >= 2)) {
            stablePhase = candidate
            stablePhaseCount = 1
            candidateCount = 0
        }
    }

    private fun decayUnknown() {
        stablePhaseCount = (stablePhaseCount - 1).coerceAtLeast(0)
        if (stablePhaseCount == 0) {
            stablePhase = TrafficLightPhase.UNKNOWN
        }
    }

    private data class Quad(
        val phase: TrafficLightPhase,
        val conf: Float,
        val redTop: Float,
        val greenBottom: Float
    )

    companion object {
        private const val TAG = "TrafficLightClassifier"
    }
}
