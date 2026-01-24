package com.owlitech.owli.assist.blindview

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockPositionMapperTest {

    @Test
    fun mapsCenterXToClockPositions() {
        assertEquals(9, ClockPositionMapper.toClock(0f))
        assertEquals(10, ClockPositionMapper.toClock(0.1f))
        assertEquals(12, ClockPositionMapper.toClock(0.5f))
        assertEquals(3, ClockPositionMapper.toClock(1f))
    }

    @Test
    fun clampsOutOfRangeValues() {
        assertEquals(9, ClockPositionMapper.toClock(-0.5f))
        assertEquals(3, ClockPositionMapper.toClock(1.5f))
    }
}
