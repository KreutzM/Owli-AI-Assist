package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer : SceneAnalyzer {
    private var lastRelevantDetectionAt: Long = 0L
    private val decayMillis: Long = 800L
    private val confidenceThreshold: Float = 0.4f

    override fun analyze(detections: List<Detection>): SceneState {
        val now = System.currentTimeMillis()

        val hazardCandidates = detections
            .filter { it.confidence >= confidenceThreshold }
            .mapNotNull { mapDetectionToHazard(it) }

        if (hazardCandidates.isEmpty()) {
            // Decay nach Timeout: zurück auf NONE
            if (lastRelevantDetectionAt != 0L && now - lastRelevantDetectionAt > decayMillis) {
                lastRelevantDetectionAt = 0L
                return SceneState(
                    timestamp = now,
                    detections = detections,
                    hazards = emptyList(),
                    primaryMessage = null,
                    overallHazardLevel = HazardLevel.NONE
                )
            }
            return SceneState(
                timestamp = now,
                detections = detections,
                hazards = emptyList(),
                primaryMessage = null,
                overallHazardLevel = HazardLevel.NONE
            )
        }

        lastRelevantDetectionAt = now
        val hazards = hazardCandidates.map { it.first }
        val overallLevel = hazards.maxOfOrNull { it.urgency } ?: HazardLevel.NONE
        val primary = hazardCandidates.maxByOrNull { it.third }

        return SceneState(
            timestamp = now,
            detections = detections,
            hazards = hazards,
            primaryMessage = primary?.second,
            overallHazardLevel = overallLevel
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
