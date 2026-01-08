package com.owlitech.owli.assist.blindview

import com.owlitech.owli.assist.ml.Detection

class BlindViewAnnouncePlanner(
    private val config: BlindViewConfig = BlindViewConfig(),
    private val translator: LabelTranslator = CocoLabelTranslator()
) {

    private val distanceEstimator = DistanceEstimator(config)
    private val distanceOrder = mapOf(
        DistanceBin.NEAR to 0,
        DistanceBin.MID to 1,
        DistanceBin.FAR to 2
    )

    fun plan(detections: List<Detection>): List<AnnouncedObject> {
        if (detections.isEmpty()) return emptyList()
        val filtered = detections.filter { it.confidence >= config.minConfidence }
        if (filtered.isEmpty()) return emptyList()

        val grouped = mutableMapOf<Key, MutableAggregate>()

        filtered.forEach { detection ->
            val bbox = detection.bbox
            val centerX = ((bbox.xMin + bbox.xMax) * 0.5f).coerceIn(0f, 1f)
            val clock = ClockPositionMapper.toClock(centerX)
            val distanceBin = distanceEstimator.estimate(bbox)
            val spokenLabel = translator.translate(detection.label)
            val key = Key(spokenLabel.en, clock, distanceBin, spokenLabel.deSingular, spokenLabel.dePlural)

            val aggregate = grouped.getOrPut(key) { MutableAggregate() }
            aggregate.count += 1
            aggregate.sumCenterX += centerX
            aggregate.maxConfidence = maxOf(aggregate.maxConfidence, detection.confidence)
        }

        return grouped.map { (key, agg) ->
            AnnouncedObject(
                labelEn = key.labelEn,
                labelDeSingular = key.labelDeSingular,
                labelDePlural = key.labelDePlural,
                count = agg.count,
                clock = key.clock,
                distance = key.distance,
                centerX = agg.sumCenterX / agg.count,
                maxConfidence = agg.maxConfidence
            )
        }.sortedWith(
            compareBy<AnnouncedObject> { distanceOrder[it.distance] ?: Int.MAX_VALUE }
                .thenBy { it.centerX }
        )
    }

    private data class Key(
        val labelEn: String,
        val clock: Int,
        val distance: DistanceBin,
        val labelDeSingular: String,
        val labelDePlural: String
    )

    private data class MutableAggregate(
        var count: Int = 0,
        var sumCenterX: Float = 0f,
        var maxConfidence: Float = 0f
    )
}
