package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer : SceneAnalyzer {
    private var lastRelevantDetectionAt: Long = 0L
    private val decayMillis: Long = 800L
    private val confidenceThreshold: Float = 0.5f

    override fun analyze(detections: List<Detection>): SceneState {
        val now = System.currentTimeMillis()

        val relevant = detections
            .filter { it.confidence >= confidenceThreshold && it.label.lowercase() in RELEVANT_LABELS }
            .maxByOrNull { it.confidence }

        if (relevant == null) {
            if (now - lastRelevantDetectionAt > decayMillis) {
                return SceneState(
                    timestamp = now,
                    detections = detections,
                    hazards = emptyList(),
                    primaryMessage = null,
                    overallHazardLevel = HazardLevel.NONE
                )
            }
            // innerhalb der Decay-Zeit: nichts Neues, behalte den letzten Status (keine neue Message)
            return SceneState(
                timestamp = now,
                detections = detections,
                hazards = emptyList(),
                primaryMessage = null,
                overallHazardLevel = HazardLevel.NONE
            )
        }

        lastRelevantDetectionAt = now
        val hazard = mapDetectionToHazard(relevant)
        val hazards = listOfNotNull(hazard)
        val message = hazard?.let { it.second }
        val overallLevel = hazard?.first?.urgency ?: HazardLevel.NONE
        return SceneState(
            timestamp = now,
            detections = detections,
            hazards = hazards.map { it.first },
            primaryMessage = message,
            overallHazardLevel = overallLevel
        )
    }

    private fun mapDetectionToHazard(detection: Detection): Pair<HazardEvent, String?>? {
        val label = detection.label.lowercase()
        return when (label) {
            "person" -> {
                HazardEvent(
                    type = HazardType.PERSON_AHEAD,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING
                ) to "Achtung, Person voraus"
            }
            "car", "truck", "bus", "motorcycle", "bicycle" -> {
                HazardEvent(
                    type = HazardType.VEHICLE_AHEAD,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING
                ) to "Achtung, Fahrzeug voraus"
            }
            "traffic light", "traffic_light", "trafficlight" -> {
                HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_RED,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.NONE
                ) to null
            }
            else -> null
        }
    }

    private companion object {
        val RELEVANT_LABELS = setOf(
            "person",
            "car",
            "truck",
            "bus",
            "motorcycle",
            "bicycle",
            "traffic light",
            "traffic_light",
            "trafficlight"
        )
    }
}
