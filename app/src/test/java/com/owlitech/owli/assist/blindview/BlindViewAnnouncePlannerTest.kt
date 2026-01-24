package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.BoundingBox
import com.owlitech.owli.assist.ml.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlindViewAnnouncePlannerTest {

    @Test
    fun filtersByConfidenceAndGroupsByClockAndDistance() {
        val config = BlindViewConfig(minConfidence = 0.5f)
        val planner = BlindViewAnnouncePlanner(
            config = config,
            translator = FixedTranslator("person")
        )
        val detections = listOf(
            det("person", 0.6f, bbox(0.2f, 0.2f, 0.8f, 0.8f)),
            det("person", 0.8f, bbox(0.22f, 0.2f, 0.82f, 0.8f)),
            det("person", 0.4f, bbox(0.2f, 0.2f, 0.8f, 0.8f))
        )

        val planned = planner.plan(detections)

        assertEquals(1, planned.size)
        val item = planned.first()
        assertEquals(2, item.count)
        assertEquals("Person", item.labelDeSingular)
        assertEquals(12, item.clock)
        assertEquals(DistanceBin.NEAR, item.distance)
        assertEquals(0.8f, item.maxConfidence, 0.0001f)
    }

    @Test
    fun sortsByDistanceThenCenterX() {
        val planner = BlindViewAnnouncePlanner(
            config = BlindViewConfig(minConfidence = 0.1f),
            translator = FixedTranslator("car")
        )
        val detections = listOf(
            det("car", 0.9f, bbox(0.0f, 0.0f, 0.4f, 0.4f)),
            det("car", 0.9f, bbox(0.6f, 0.0f, 1.0f, 0.4f)),
            det("car", 0.9f, bbox(0.45f, 0.45f, 0.55f, 0.55f))
        )

        val planned = planner.plan(detections)

        assertEquals(3, planned.size)
        assertEquals(DistanceBin.NEAR, planned[0].distance)
        assertEquals(DistanceBin.NEAR, planned[1].distance)
        assertEquals(DistanceBin.FAR, planned[2].distance)
        assertTrue(planned[0].centerX < planned[1].centerX)
    }

    private fun det(label: String, confidence: Float, bbox: BoundingBox): Detection {
        return Detection(label = label, confidence = confidence, bbox = bbox)
    }

    private fun bbox(xMin: Float, yMin: Float, xMax: Float, yMax: Float): BoundingBox {
        return BoundingBox(xMin, yMin, xMax, yMax)
    }

    private class FixedTranslator(private val label: String) : LabelTranslator {
        override fun translate(en: String): SpokenLabel {
            val name = if (en == label) "Person" else "Objekt"
            return SpokenLabel(en, name, "${name}en")
        }
    }
}
