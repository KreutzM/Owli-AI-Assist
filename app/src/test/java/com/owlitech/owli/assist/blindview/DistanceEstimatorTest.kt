package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.BoundingBox
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceEstimatorTest {

    private val estimator = DistanceEstimator(BlindViewConfig())

    @Test
    fun classifiesNearWhenAreaExceedsNearThreshold() {
        val bbox = BoundingBox(0f, 0f, 0.4f, 0.4f)
        assertEquals(DistanceBin.NEAR, estimator.estimate(bbox))
    }

    @Test
    fun classifiesMidWhenAreaBetweenThresholds() {
        val bbox = BoundingBox(0f, 0f, 0.25f, 0.2f)
        assertEquals(DistanceBin.MID, estimator.estimate(bbox))
    }

    @Test
    fun classifiesFarWhenAreaBelowMidThreshold() {
        val bbox = BoundingBox(0f, 0f, 0.1f, 0.1f)
        assertEquals(DistanceBin.FAR, estimator.estimate(bbox))
    }
}
