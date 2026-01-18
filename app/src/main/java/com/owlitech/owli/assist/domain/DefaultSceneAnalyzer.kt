package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.blindview.AnnouncedObject
import com.owlitech.owli.assist.blindview.BlindViewAnnouncePlanner
import com.owlitech.owli.assist.blindview.BlindViewConfig
import com.owlitech.owli.assist.blindview.BlindViewUtteranceFormatter
import com.owlitech.owli.assist.blindview.ClockPositionMapper
import com.owlitech.owli.assist.blindview.CocoLabelTranslator
import com.owlitech.owli.assist.blindview.DistanceBin
import com.owlitech.owli.assist.blindview.IouObjectTracker
import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.domain.TrafficLightObservation
import com.owlitech.owli.assist.domain.TrafficLightPhase
import com.owlitech.owli.assist.motion.MotionLevel
import com.owlitech.owli.assist.motion.MotionSnapshot

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer(
    private val blindViewConfig: BlindViewConfig = BlindViewConfig(),
    translator: CocoLabelTranslator = CocoLabelTranslator(),
    private val motionGatingEnabled: Boolean = true,
    private val motionSpeakIntervalMultiplierHigh: Float = 1.35f
) : SceneAnalyzer {
    private var lastRelevantDetectionAt: Long = 0L
    private val decayMillis: Long = 800L
    private val confidenceThreshold: Float = 0.4f
    private var lastStableTrafficLight: TrafficLightPhase = TrafficLightPhase.UNKNOWN
    private val announcePlanner = BlindViewAnnouncePlanner(
        config = blindViewConfig,
        translator = translator
    )
    private val tracker = IouObjectTracker(blindViewConfig)
    private val directionMemory = mutableMapOf<DirectionKey, DirectionMemory>()
    private var lastBlindViewEmitAt: Long = 0L

    override fun analyze(
        detections: List<Detection>,
        trafficLights: List<TrafficLightObservation>,
        motion: MotionSnapshot?
    ): SceneState {
        val now = System.currentTimeMillis()
        val motionLevel = if (motionGatingEnabled) motion?.motionLevel else null
        val effectiveTrackMaxAgeMs = if (motionLevel == MotionLevel.HIGH) {
            (blindViewConfig.trackMaxAgeMs * HIGH_MOTION_TRACK_AGE_MULTIPLIER).toLong()
        } else {
            blindViewConfig.trackMaxAgeMs
        }
        val effectiveSmoothingAlpha = if (motionLevel == MotionLevel.HIGH) {
            (blindViewConfig.bboxSmoothingAlpha * HIGH_MOTION_SMOOTHING_ALPHA_SCALE).coerceIn(0f, 1f)
        } else {
            blindViewConfig.bboxSmoothingAlpha
        }
        val stableDetections = tracker.update(
            detections = detections,
            nowMs = now,
            maxAgeOverrideMs = effectiveTrackMaxAgeMs,
            smoothingAlphaOverride = effectiveSmoothingAlpha
        )
        val plannedItems = announcePlanner.plan(stableDetections)
        val stabilizedItems = applyDirectionHysteresis(plannedItems, now, motionLevel)
        val gatedItems = applyMotionSpeakGate(stabilizedItems, now, motionLevel)
        val blindViewItems = gatedItems
        val blindViewUtterance = BlindViewUtteranceFormatter.format(
            blindViewItems,
            blindViewConfig.maxItemsSpoken
        )

        val hazardCandidates = detections
            .filter { it.confidence >= confidenceThreshold }
            .mapNotNull { mapDetectionToHazard(it) }

        val tlPrimary = trafficLights.firstOrNull()
        val tlHazard = tlPrimary?.let {
            when (it.phase) {
                TrafficLightPhase.RED -> HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_RED,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING
                )
                TrafficLightPhase.GREEN -> HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_GREEN,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.NONE
                )
                else -> null
            }
        }

        val combinedHazards = buildList {
            addAll(hazardCandidates.map { it.first })
            tlHazard?.let { add(it) }
        }

        if (combinedHazards.isEmpty()) {
            // Decay nach Timeout: zurÃ¼ck auf NONE
            if (lastRelevantDetectionAt != 0L && now - lastRelevantDetectionAt > decayMillis) {
                lastRelevantDetectionAt = 0L
                return SceneState(
                    timestamp = now,
                    detections = detections,
                    hazards = emptyList(),
                    primaryMessage = null,
                    overallHazardLevel = HazardLevel.NONE,
                    trafficLights = trafficLights,
                    primaryTrafficLight = tlPrimary?.phase,
                    blindViewItems = blindViewItems,
                    blindViewUtterancePreview = blindViewUtterance
                )
            }
            return SceneState(
                timestamp = now,
                detections = detections,
                hazards = emptyList(),
                primaryMessage = null,
                overallHazardLevel = HazardLevel.NONE,
                trafficLights = trafficLights,
                primaryTrafficLight = tlPrimary?.phase,
                blindViewItems = blindViewItems,
                blindViewUtterancePreview = blindViewUtterance
            )
        }

        lastRelevantDetectionAt = now
        val overallLevel = combinedHazards.maxOfOrNull { it.urgency } ?: HazardLevel.NONE
        val primary = hazardCandidates.maxByOrNull { it.third }

        val primaryMessage = buildString {
            when (tlPrimary?.phase) {
                TrafficLightPhase.RED -> {
                    append("Ampel rot")
                    lastStableTrafficLight = TrafficLightPhase.RED
                }
                TrafficLightPhase.GREEN -> {
                    if (lastStableTrafficLight == TrafficLightPhase.RED) {
                        append("Ampel grÃ¼n")
                    }
                    lastStableTrafficLight = TrafficLightPhase.GREEN
                }
                TrafficLightPhase.UNKNOWN, null -> {
                    if (primary != null && primary.second != null) {
                        append(primary.second)
                    }
                }
            }
        }.ifEmpty { primary?.second }

        return SceneState(
            timestamp = now,
            detections = detections,
            hazards = combinedHazards,
            primaryMessage = primaryMessage,
            overallHazardLevel = overallLevel,
            trafficLights = trafficLights,
            primaryTrafficLight = tlPrimary?.phase,
            blindViewItems = blindViewItems,
            blindViewUtterancePreview = blindViewUtterance
        )
    }

    private fun mapDetectionToHazard(detection: Detection): Triple<HazardEvent, String?, Float>? {
        val label = detection.label.lowercase()
        return when (label) {
            "person" -> Triple(
                HazardEvent(
                    type = HazardType.PERSON_AHEAD,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING
                ),
                "Achtung, Person voraus",
                detection.confidence
            )
            "car", "truck", "bus", "motorcycle", "bicycle" -> Triple(
                HazardEvent(
                    type = HazardType.VEHICLE_AHEAD,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING
                ),
                "Achtung, Fahrzeug voraus",
                detection.confidence
            )
            "traffic light", "traffic_light", "trafficlight" -> Triple(
                HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_RED,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.NONE
                ),
                null,
                detection.confidence
            )
            else -> null
        }
    }

    private fun applyMotionSpeakGate(
        items: List<AnnouncedObject>,
        nowMs: Long,
        motionLevel: MotionLevel?
    ): List<AnnouncedObject> {
        if (items.isEmpty()) return items
        if (motionLevel != MotionLevel.HIGH || !motionGatingEnabled) {
            lastBlindViewEmitAt = nowMs
            return items
        }
        val effectiveInterval =
            (blindViewConfig.minSpeakIntervalMs * motionSpeakIntervalMultiplierHigh.coerceAtLeast(1f)).toLong()
        if (nowMs - lastBlindViewEmitAt < effectiveInterval) {
            return emptyList()
        }
        lastBlindViewEmitAt = nowMs
        return items
    }

    private fun applyDirectionHysteresis(
        items: List<AnnouncedObject>,
        nowMs: Long,
        motionLevel: MotionLevel?
    ): List<AnnouncedObject> {
        if (items.isEmpty()) return items
        pruneDirectionMemory(nowMs)
        if (motionLevel != MotionLevel.HIGH || !motionGatingEnabled) {
            items.forEach { item ->
                directionMemory[DirectionKey(item.labelEn, item.distance)] =
                    DirectionMemory(item.centerX, nowMs)
            }
            return items
        }
        val alpha = HIGH_MOTION_DIRECTION_ALPHA
        return items.map { item ->
            val key = DirectionKey(item.labelEn, item.distance)
            val previous = directionMemory[key]
            val smoothedCenter = if (previous == null) {
                item.centerX
            } else {
                previous.centerX + (item.centerX - previous.centerX) * alpha
            }
            directionMemory[key] = DirectionMemory(smoothedCenter, nowMs)
            val clock = ClockPositionMapper.toClock(smoothedCenter)
            if (clock == item.clock && smoothedCenter == item.centerX) {
                item
            } else {
                item.copy(clock = clock, centerX = smoothedCenter)
            }
        }
    }

    private fun pruneDirectionMemory(nowMs: Long) {
        val maxAge = blindViewConfig.trackMaxAgeMs.coerceAtLeast(DIRECTION_MEMORY_MIN_AGE_MS)
        val iterator = directionMemory.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowMs - entry.value.lastSeenAt > maxAge) {
                iterator.remove()
            }
        }
    }

    private data class DirectionKey(
        val label: String,
        val distance: DistanceBin
    )

    private data class DirectionMemory(
        val centerX: Float,
        val lastSeenAt: Long
    )

    companion object {
        private const val HIGH_MOTION_TRACK_AGE_MULTIPLIER = 1.4f
        private const val HIGH_MOTION_SMOOTHING_ALPHA_SCALE = 0.55f
        private const val HIGH_MOTION_DIRECTION_ALPHA = 0.25f
        private const val DIRECTION_MEMORY_MIN_AGE_MS = 1_200L
    }
}
