package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.ml.BoundingBox
import com.owlitech.owli.assist.ml.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultSceneAnalyzerTest {

    private val analyzer = DefaultSceneAnalyzer()

    @Test
    fun `returns NONE when no detections`() {
        val state = analyzer.analyze(emptyList(), emptyList(), null)

        assertEquals(HazardLevel.NONE, state.overallHazardLevel)
        assertEquals(0, state.hazards.size)
        assertNull(state.primaryMessage)
    }

    @Test
    fun `returns WARNING and message when detection present`() {
        val detections = listOf(
            Detection(
                label = "person",
                confidence = 0.9f,
                bbox = BoundingBox(0.2f, 0.2f, 0.4f, 0.6f)
            )
        )

        val state = analyzer.analyze(detections, emptyList(), null)

        assertEquals(HazardLevel.WARNING, state.overallHazardLevel)
        assertEquals(1, state.hazards.size)
        assertEquals(HazardType.PERSON_AHEAD, state.hazards.first().type)
        assertEquals("Achtung, Person voraus", state.primaryMessage)
    }
}
