package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.BoundingBox
import com.owlitech.owli.assist.ml.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IouObjectTrackerTest {

    @Test
    fun announcesAfterConsecutiveHits() {
        val config = BlindViewConfig(
            minConsecutiveHitsToAnnounce = 2,
            minConfidenceTrack = 0.1f,
            minBboxAreaForTracking = 0f,
            iouThreshold = 0.2f,
            confidenceEmaAlpha = 1f,
            confidenceDecayPerSecond = 0f
        )
        val tracker = IouObjectTracker(config)
        val detection = det("person", BoundingBox(0f, 0f, 0.2f, 0.2f))

        val first = tracker.update(listOf(detection), nowMs = 0L)
        val second = tracker.update(listOf(detection), nowMs = 100L)

        assertTrue(first.isEmpty())
        assertEquals(1, second.size)
        assertEquals("person", second.first().label)
    }

    @Test
    fun smoothsBoundingBoxWhenMatched() {
        val config = BlindViewConfig(
            minConsecutiveHitsToAnnounce = 1,
            minConfidenceTrack = 0.1f,
            minBboxAreaForTracking = 0f,
            iouThreshold = 0.1f,
            confidenceEmaAlpha = 1f,
            confidenceDecayPerSecond = 0f
        )
        val tracker = IouObjectTracker(config)
        val first = det("car", BoundingBox(0f, 0f, 1f, 1f))
        val second = det("car", BoundingBox(0f, 0f, 0.5f, 0.5f))

        tracker.update(listOf(first), nowMs = 0L)
        val smoothed = tracker.update(listOf(second), nowMs = 100L, smoothingAlphaOverride = 0.5f)

        val bbox = smoothed.first().bbox
        assertEquals(0.75f, bbox.xMax, 0.0001f)
        assertEquals(0.75f, bbox.yMax, 0.0001f)
    }

    @Test
    fun prunesTracksAfterMaxAge() {
        val config = BlindViewConfig(
            minConsecutiveHitsToAnnounce = 1,
            minConfidenceTrack = 0.1f,
            minBboxAreaForTracking = 0f,
            trackMaxAgeMs = 100L,
            confidenceEmaAlpha = 1f,
            confidenceDecayPerSecond = 0f
        )
        val tracker = IouObjectTracker(config)
        val detection = det("person", BoundingBox(0f, 0f, 0.2f, 0.2f))

        tracker.update(listOf(detection), nowMs = 0L)
        val afterGap = tracker.update(emptyList(), nowMs = 200L)

        assertTrue(afterGap.isEmpty())
    }

    private fun det(label: String, bbox: BoundingBox): Detection {
        return Detection(label = label, confidence = 0.9f, bbox = bbox)
    }
}
