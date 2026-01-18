package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.BoundingBox
import com.owlitech.owli.assist.ml.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * Einfacher IoU-basierter Tracker pro Label mit geglätteten Bounding Boxes.
 */
class IouObjectTracker(
    private val config: BlindViewConfig
) {
    private val tracks = mutableListOf<Track>()
    private var nextId: Long = 1
    private var lastUpdateAt: Long = 0L

    fun update(
        detections: List<Detection>,
        nowMs: Long,
        maxAgeOverrideMs: Long? = null,
        smoothingAlphaOverride: Float? = null
    ): List<Detection> {
        val effectiveMaxAge = maxAgeOverrideMs ?: config.trackMaxAgeMs
        val effectiveSmoothingAlpha = smoothingAlphaOverride ?: config.bboxSmoothingAlpha
        val prevUpdateAt = lastUpdateAt
        if (config.resetAfterGapMs > 0 && lastUpdateAt != 0L && nowMs - lastUpdateAt > config.resetAfterGapMs) {
            tracks.clear()
        }
        lastUpdateAt = nowMs
        val dtSeconds = if (prevUpdateAt == 0L) 0f else ((nowMs - prevUpdateAt).toFloat() / 1000f).coerceAtLeast(0f)

        if (detections.isEmpty()) {
            pruneOld(nowMs, effectiveMaxAge)
            val allIds = tracks.map { it.id }.toSet()
            decayUnmatched(dtSeconds, allIds)
            return stableTracks(nowMs, effectiveMaxAge)
        }

        val filtered = detections
            .asSequence()
            .filter { it.confidence >= config.minConfidenceTrack }
            .filter {
                val area = bboxArea(it.bbox)
                area >= config.minBboxAreaForTracking
            }
            .sortedByDescending { it.confidence }
            .take(config.maxDetectionsPerFrameForTracking)
            .toList()

        val unmatchedTracks = tracks.map { it.id }.toMutableSet()
        filtered.forEach { detection ->
            val match = findBestMatch(detection, unmatchedTracks)
            if (match != null) {
                unmatchedTracks.remove(match.id)
                smoothTrack(match, detection, nowMs, effectiveSmoothingAlpha)
            } else {
                createTrack(detection, nowMs)
            }
        }

        decayUnmatched(dtSeconds, unmatchedTracks)
        pruneOld(nowMs, effectiveMaxAge)
        pruneTrackLimit()
        return stableTracks(nowMs, effectiveMaxAge)
    }

    private fun findBestMatch(detection: Detection, unmatched: Set<Long>): Track? {
        var best: Track? = null
        var bestIou = 0f
        tracks.forEach { track ->
            if (track.label != detection.label) return@forEach
            if (!unmatched.contains(track.id)) return@forEach
            val iou = iou(track.bbox, detection.bbox)
            if (iou >= config.iouThreshold && iou > bestIou) {
                bestIou = iou
                best = track
            }
        }
        return best
    }

    private fun smoothTrack(track: Track, detection: Detection, nowMs: Long, smoothingAlpha: Float) {
        val a = smoothingAlpha.coerceIn(0f, 1f)
        val confAlpha = config.confidenceEmaAlpha.coerceIn(0f, 1f)
        track.bbox = lerp(track.bbox, detection.bbox, a)
        track.confidenceEma = ema(track.confidenceEma, detection.confidence, confAlpha)
        track.lastConfidence = detection.confidence
        track.consecutiveHits += 1
        track.hits += 1
        track.lastSeenAt = nowMs
    }

    private fun createTrack(detection: Detection, nowMs: Long) {
        tracks.add(
            Track(
                id = nextId++,
                label = detection.label,
                bbox = detection.bbox,
                confidenceEma = detection.confidence,
                lastConfidence = detection.confidence,
                hits = 1,
                consecutiveHits = 1,
                lastSeenAt = nowMs,
                createdAt = nowMs
            )
        )
    }

    private fun pruneOld(nowMs: Long, maxAge: Long) {
        tracks.removeAll { nowMs - it.lastSeenAt > maxAge }
    }

    private fun decayUnmatched(dtSeconds: Float, unmatched: Set<Long>) {
        val decayPerSecond = config.confidenceDecayPerSecond.coerceAtLeast(0f)
        tracks.forEach { track ->
            if (unmatched.contains(track.id)) {
                track.consecutiveHits = 0
                val decay = decayPerSecond * dtSeconds
                track.confidenceEma = max(0f, track.confidenceEma - decay)
            }
        }
    }

    private fun stableTracks(nowMs: Long, maxAge: Long): List<Detection> {
        return tracks.filter { track ->
            track.consecutiveHits >= config.minConsecutiveHitsToAnnounce &&
                track.confidenceEma >= config.minConfidenceTrack &&
                nowMs - track.lastSeenAt <= maxAge
        }.map { track ->
            Detection(
                label = track.label,
                confidence = track.confidenceEma,
                bbox = track.bbox
            )
        }
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val xMin = max(a.xMin, b.xMin)
        val yMin = max(a.yMin, b.yMin)
        val xMax = min(a.xMax, b.xMax)
        val yMax = min(a.yMax, b.yMax)
        val interW = (xMax - xMin).coerceAtLeast(0f)
        val interH = (yMax - yMin).coerceAtLeast(0f)
        val interArea = interW * interH
        if (interArea <= 0f) return 0f
        val areaA = bboxArea(a)
        val areaB = bboxArea(b)
        val union = areaA + areaB - interArea
        if (union <= 0f) return 0f
        return interArea / union
    }

    private fun lerp(old: BoundingBox, new: BoundingBox, alpha: Float): BoundingBox {
        val inv = 1f - alpha
        return BoundingBox(
            xMin = old.xMin * inv + new.xMin * alpha,
            yMin = old.yMin * inv + new.yMin * alpha,
            xMax = old.xMax * inv + new.xMax * alpha,
            yMax = old.yMax * inv + new.yMax * alpha
        )
    }

    private fun ema(old: Float, new: Float, alpha: Float): Float {
        val a = alpha.coerceIn(0f, 1f)
        val inv = 1f - a
        return old * inv + new * a
    }

    private fun bboxArea(bbox: BoundingBox): Float {
        return (bbox.xMax - bbox.xMin).coerceAtLeast(0f) * (bbox.yMax - bbox.yMin).coerceAtLeast(0f)
    }

    private fun pruneTrackLimit() {
        val maxTracks = config.maxTracks
        if (tracks.size <= maxTracks) return
        val toRemove = tracks.size - maxTracks
        val sorted = tracks.sortedWith(
            compareBy<Track> { it.consecutiveHits }
                .thenBy { it.confidenceEma }
                .thenBy { it.lastSeenAt }
        )
        val idsToRemove = sorted.take(toRemove).map { it.id }.toSet()
        tracks.removeAll { idsToRemove.contains(it.id) }
    }

    private data class Track(
        val id: Long,
        val label: String,
        var bbox: BoundingBox,
        var confidenceEma: Float,
        var lastConfidence: Float,
        var hits: Int,
        var consecutiveHits: Int,
        var lastSeenAt: Long,
        val createdAt: Long
    )
}
