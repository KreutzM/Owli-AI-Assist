package com.example.bikeassist.domain

import com.example.bikeassist.ml.BoundingBox

private const val DIR_LEFT_MAX = 0.33f
private const val DIR_RIGHT_MIN = 0.66f
private const val ZONE_NEAR_MIN = 0.75f
private const val ZONE_MID_MIN = 0.55f

fun directionFromBox(b: BoundingBox): Direction {
    val centerX = (b.xMin + b.xMax) / 2f
    return when {
        centerX < DIR_LEFT_MAX -> Direction.LEFT
        centerX > DIR_RIGHT_MIN -> Direction.RIGHT
        else -> Direction.CENTER
    }
}

fun zoneFromBox(b: BoundingBox): ProximityZone {
    val bottomY = b.yMax
    return when {
        bottomY >= ZONE_NEAR_MIN -> ProximityZone.NEAR
        bottomY >= ZONE_MID_MIN -> ProximityZone.MID
        else -> ProximityZone.FAR
    }
}
