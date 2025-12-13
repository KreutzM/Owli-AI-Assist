package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection
import com.example.bikeassist.domain.TrafficLightObservation
import com.example.bikeassist.domain.TrafficLightPhase

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer : SceneAnalyzer {
    private var lastRelevantDetectionAt: Long = 0L
    private val decayMillis: Long = 800L
    private val confidenceThreshold: Float = 0.4f

    override fun analyze(detections: List<Detection>, trafficLights: List<TrafficLightObservation>): SceneState {
        val now = System.currentTimeMillis()

        val hazardCandidates = detections
            .filter { it.confidence >= confidenceThreshold }
            .mapNotNull { mapDetectionToHazard(it) }

        val tlPrimary = trafficLights.maxByOrNull { it.confidence }
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
                    primaryTrafficLight = tlPrimary?.phase
                )
            }
            return SceneState(
                timestamp = now,
                detections = detections,
                hazards = emptyList(),
                primaryMessage = null,
                overallHazardLevel = HazardLevel.NONE,
                trafficLights = trafficLights,
                primaryTrafficLight = tlPrimary?.phase
            )
        }

        lastRelevantDetectionAt = now
        val overallLevel = combinedHazards.maxOfOrNull { it.urgency } ?: HazardLevel.NONE
        val primary = hazardCandidates.maxByOrNull { it.third }

        val primaryMessage = when {
            tlPrimary?.phase == TrafficLightPhase.RED -> "Ampel rot"
            tlPrimary?.phase == TrafficLightPhase.GREEN -> primary?.second ?: "Ampel grün"
            else -> primary?.second
        }

        return SceneState(
            timestamp = now,
            detections = detections,
            hazards = combinedHazards,
            primaryMessage = primaryMessage,
            overallHazardLevel = overallLevel,
            trafficLights = trafficLights,
            primaryTrafficLight = tlPrimary?.phase
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
