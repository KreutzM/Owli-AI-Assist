package com.owlitech.owli.assist.motion

enum class MotionLevel {
    LOW,
    MED,
    HIGH
}

data class MotionSnapshot(
    val timestampNs: Long,
    val rollRad: Float,
    val pitchRad: Float,
    val yawRad: Float?,
    val gyroMagRadS: Float,
    val motionLevel: MotionLevel,
    val quality: Float
)
