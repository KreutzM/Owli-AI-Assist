package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.BoundingBox

class DistanceEstimator(
    private val config: BlindViewConfig
) {
    fun estimate(bbox: BoundingBox): DistanceBin {
        val area = (bbox.xMax - bbox.xMin) * (bbox.yMax - bbox.yMin)
        return when {
            area >= config.nearThreshold -> DistanceBin.NEAR
            area >= config.midThreshold -> DistanceBin.MID
            else -> DistanceBin.FAR
        }
    }
}
