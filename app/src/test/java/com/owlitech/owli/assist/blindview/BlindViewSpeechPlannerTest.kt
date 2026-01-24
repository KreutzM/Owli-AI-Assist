package com.owlitech.owli.assist.blindview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BlindViewSpeechPlannerTest {

    private val config = BlindViewConfig(
        minSpeakIntervalMs = 1_000L,
        repeatSamePlanIntervalMs = 3_000L
    )

    @Test
    fun returnsNullWhenNoItems() {
        val planner = BlindViewSpeechPlanner(config)
        val result = planner.nextUtterance(emptyList(), now = 0L)
        assertNull(result)
    }

    @Test
    fun gatesRepeatsUntilRepeatInterval() {
        val planner = BlindViewSpeechPlanner(config)
        val items = listOf(item("person", 12))

        val first = planner.nextUtterance(items, now = 10_000L)
        val blocked = planner.nextUtterance(items, now = 10_500L)
        val allowed = planner.nextUtterance(items, now = 13_500L)

        assertNotNull(first)
        assertNull(blocked)
        assertNotNull(allowed)
    }

    @Test
    fun gatesNewPlanUntilCooldownExpires() {
        val planner = BlindViewSpeechPlanner(config)
        val itemsA = listOf(item("person", 12))
        val itemsB = listOf(item("car", 3))

        val first = planner.nextUtterance(itemsA, now = 10_000L)
        val blocked = planner.nextUtterance(itemsB, now = 10_500L)
        val allowed = planner.nextUtterance(itemsB, now = 11_500L)

        assertNotNull(first)
        assertNull(blocked)
        assertNotNull(allowed)
    }

    @Test
    fun usesFormattedUtteranceForPlanHash() {
        val planner = BlindViewSpeechPlanner(config)
        val items = listOf(item("person", 12))

        val first = planner.nextUtterance(items, now = 10_000L)

        assertEquals("Person, 12 Uhr.", first)
    }

    private fun item(labelEn: String, clock: Int): AnnouncedObject {
        val (deSingular, dePlural) = when (labelEn) {
            "car" -> "Auto" to "Autos"
            else -> "Person" to "Personen"
        }
        return AnnouncedObject(
            labelEn = labelEn,
            labelDeSingular = deSingular,
            labelDePlural = dePlural,
            count = 1,
            clock = clock,
            distance = DistanceBin.NEAR,
            centerX = 0.5f,
            maxConfidence = 0.9f
        )
    }
}
