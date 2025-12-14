package com.example.bikeassist.blindview

import kotlin.math.roundToInt

object ClockPositionMapper {
    private val positions = listOf(9, 10, 11, 12, 1, 2, 3)

    fun toClock(centerX: Float): Int {
        val idx = (centerX * 6f).roundToInt().coerceIn(0, positions.lastIndex)
        return positions[idx]
    }
}
