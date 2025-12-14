package com.example.bikeassist.blindview

object BlindViewUtteranceFormatter {
    fun format(items: List<AnnouncedObject>, maxItems: Int): String? {
        val limited = items.take(maxItems)
        if (limited.isEmpty()) return null
        val body = limited.joinToString(separator = ". ") { item ->
            if (item.count > 1) {
                "${item.count} ${item.labelDePlural}, ${item.clock} Uhr"
            } else {
                "${item.labelDeSingular}, ${item.clock} Uhr"
            }
        }
        return "$body."
    }
}
