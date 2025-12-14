package com.example.bikeassist.blindview

class BlindViewSpeechPlanner(
    private val config: BlindViewConfig = BlindViewConfig()
) {

    private var lastHash: String? = null
    private var lastSpokenAt: Long = 0L

    fun buildUtteranceText(items: List<AnnouncedObject>): String? =
        BlindViewUtteranceFormatter.format(items, config.maxItemsSpoken)

    fun nextUtterance(items: List<AnnouncedObject>, now: Long = System.currentTimeMillis()): String? {
        if (items.isEmpty()) return null
        val hash = planHash(items)
        val samePlan = hash == lastHash
        val sinceLast = now - lastSpokenAt
        val cooldownPassed = sinceLast >= config.minSpeakIntervalMs
        val repeatAllowed = sinceLast >= config.repeatSamePlanIntervalMs

        if (!samePlan && !cooldownPassed) {
            return null
        }
        if (samePlan && !repeatAllowed) {
            return null
        }

        val utterance = BlindViewUtteranceFormatter.format(items, config.maxItemsSpoken) ?: return null
        lastHash = hash
        lastSpokenAt = now
        return utterance
    }

    private fun planHash(items: List<AnnouncedObject>): String {
        return items.take(config.maxItemsSpoken).joinToString(separator = "|") {
            "${it.labelEn}:${it.clock}:${it.distance}:${it.count}"
        }
    }
}
