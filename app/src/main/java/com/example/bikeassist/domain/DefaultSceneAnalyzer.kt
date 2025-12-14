package com.example.bikeassist.domain

import com.example.bikeassist.blindview.BlindViewAnnouncePlanner
import com.example.bikeassist.blindview.BlindViewConfig
import com.example.bikeassist.blindview.BlindViewUtteranceFormatter
import com.example.bikeassist.blindview.CocoLabelTranslator
import com.example.bikeassist.blindview.IouObjectTracker
import com.example.bikeassist.ml.Detection
import com.example.bikeassist.domain.TrafficLightObservation
import com.example.bikeassist.domain.TrafficLightPhase

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer(
    private val blindViewConfig: BlindViewConfig = BlindViewConfig(),
    translator: CocoLabelTranslator = CocoLabelTranslator()
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

    override fun analyze(detections: List<Detection>, trafficLights: List<TrafficLightObservation>): SceneState {
        val now = System.currentTimeMillis()
        val stableDetections = tracker.update(detections, now)
        val blindViewItems = announcePlanner.plan(stableDetections)
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
            // Decay nach Timeout: zurück auf NONE
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
                        append("Ampel grün")
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
}
