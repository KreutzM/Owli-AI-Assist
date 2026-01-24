package com.owlitech.owli.assist.blindview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlindViewUtteranceFormatterTest {

    @Test
    fun returnsNullForEmptyItems() {
        assertNull(BlindViewUtteranceFormatter.format(emptyList(), maxItems = 3))
    }

    @Test
    fun formatsSingleItemWithSingularLabel() {
        val item = AnnouncedObject(
            labelEn = "person",
            labelDeSingular = "Person",
            labelDePlural = "Personen",
            count = 1,
            clock = 12,
            distance = DistanceBin.NEAR,
            centerX = 0.5f,
            maxConfidence = 0.9f
        )

        val result = BlindViewUtteranceFormatter.format(listOf(item), maxItems = 3)

        assertEquals("Person, 12 Uhr.", result)
    }

    @Test
    fun formatsMultipleItemsWithCountsAndSeparators() {
        val car = AnnouncedObject(
            labelEn = "car",
            labelDeSingular = "Auto",
            labelDePlural = "Autos",
            count = 2,
            clock = 3,
            distance = DistanceBin.MID,
            centerX = 0.8f,
            maxConfidence = 0.7f
        )
        val person = AnnouncedObject(
            labelEn = "person",
            labelDeSingular = "Person",
            labelDePlural = "Personen",
            count = 1,
            clock = 12,
            distance = DistanceBin.NEAR,
            centerX = 0.5f,
            maxConfidence = 0.9f
        )

        val result = BlindViewUtteranceFormatter.format(listOf(car, person), maxItems = 3)

        assertEquals("2 Autos, 3 Uhr. Person, 12 Uhr.", result)
    }

    @Test
    fun limitsToMaxItems() {
        val items = listOf(
            AnnouncedObject("car", "Auto", "Autos", 1, 9, DistanceBin.FAR, 0.1f, 0.6f),
            AnnouncedObject("person", "Person", "Personen", 1, 12, DistanceBin.NEAR, 0.5f, 0.9f),
            AnnouncedObject("bicycle", "Fahrrad", "Fahrraeder", 1, 2, DistanceBin.MID, 0.7f, 0.8f)
        )

        val result = BlindViewUtteranceFormatter.format(items, maxItems = 2)

        assertEquals("Auto, 9 Uhr. Person, 12 Uhr.", result)
    }
}
