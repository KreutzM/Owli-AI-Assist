package com.example.bikeassist.processing

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.bikeassist.domain.TrafficLightObservation
import com.example.bikeassist.domain.TrafficLightPhase
import com.example.bikeassist.ml.BoundingBox
import com.example.bikeassist.ml.Detection
import kotlin.math.max

interface TrafficLightPhaseClassifier {
    fun classify(frame: Bitmap, detections: List<Detection>): List<TrafficLightObservation>
}

class HsvTrafficLightPhaseClassifier(
    private val saturationThreshold: Float = 0.4f,
    private val valueThreshold: Float = 0.4f,
    private val minScore: Float = 0.08f,
    private val dominanceRatio: Float = 1.3f,
    private val logInterval: Int = 60
) : TrafficLightPhaseClassifier {

    private var frameCount = 0
    private var stablePhase: TrafficLightPhase = TrafficLightPhase.UNKNOWN
    private var stablePhaseCount: Int = 0

    override fun classify(frame: Bitmap, detections: List<Detection>): List<TrafficLightObservation> {
        frameCount++
        val trafficDetections = detections.filter { it.label.equals("traffic light", ignoreCase = true) }
        if (trafficDetections.isEmpty()) {
            stablePhase = TrafficLightPhase.UNKNOWN
            stablePhaseCount = 0
            return emptyList()
        }

        val observations = trafficDetections.mapNotNull { det ->
            val bbox = det.bbox
            val roi = crop(frame, bbox) ?: return@mapNotNull null
            val phaseScore = scorePhase(roi)
            TrafficLightObservation(
                bbox = bbox,
                phase = phaseScore.first,
                confidence = phaseScore.second
            )
        }

        val primary = observations.maxByOrNull { it.confidence }
        primary?.let { updateStable(it.phase) }

        if (frameCount % logInterval == 0 && primary != null) {
            Log.d(TAG, "TL classify primary=${primary.phase} conf=${primary.confidence} stable=$stablePhase count=$stablePhaseCount")
        }

        return observations
    }

    private fun updateStable(phase: TrafficLightPhase) {
        if (phase == stablePhase) {
            stablePhaseCount = (stablePhaseCount + 1).coerceAtMost(3)
        } else {
            if (phase != TrafficLightPhase.UNKNOWN) {
                stablePhase = phase
                stablePhaseCount = 1
            } else {
                stablePhaseCount = max(0, stablePhaseCount - 1)
                if (stablePhaseCount == 0) {
                    stablePhase = TrafficLightPhase.UNKNOWN
                }
            }
        }
    }

    private fun scorePhase(bitmap: Bitmap): Pair<TrafficLightPhase, Float> {
        var redCount = 0
        var greenCount = 0
        var valid = 0
        val hsv = FloatArray(3)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (p in pixels) {
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            val hue = hsv[0]
            if (sat < saturationThreshold || value < valueThreshold) continue
            valid++
            if (hue <= 15f || hue >= 345f) {
                redCount++
            } else if (hue in 80f..160f) {
                greenCount++
            }
        }
        if (valid == 0) return TrafficLightPhase.UNKNOWN to 0f
        val redScore = redCount.toFloat() / valid
        val greenScore = greenCount.toFloat() / valid
        val phase = when {
            redScore > minScore && redScore > greenScore * dominanceRatio -> TrafficLightPhase.RED
            greenScore > minScore && greenScore > redScore * dominanceRatio -> TrafficLightPhase.GREEN
            else -> TrafficLightPhase.UNKNOWN
        }
        val conf = max(redScore, greenScore).coerceIn(0f, 1f)
        return phase to conf
    }

    private fun crop(frame: Bitmap, bbox: BoundingBox): Bitmap? {
        val left = (bbox.xMin * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (bbox.yMin * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (bbox.xMax * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (bbox.yMax * frame.height).toInt().coerceIn(top + 1, frame.height)
        val width = right - left
        val height = bottom - top
        if (width < 4 || height < 4) return null
        return Bitmap.createBitmap(frame, left, top, width, height)
    }

    companion object {
        private const val TAG = "TrafficLightClassifier"
    }
}
