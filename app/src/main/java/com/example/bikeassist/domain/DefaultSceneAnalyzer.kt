package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection
import kotlin.math.max

/**
 * Heuristiken für Hazards inkl. Richtung/Zone und Ampel-Integration.
 */
class DefaultSceneAnalyzer : SceneAnalyzer {
    private var lastRelevantDetectionAt: Long = 0L
    private val decayMillis: Long = 800L
    private val confidenceThreshold: Float = 0.4f
    private var lastStableTrafficLight: TrafficLightPhase = TrafficLightPhase.UNKNOWN
    private var lastPoleLike: Boolean = false
    private var lastAspectRatio: Float = 0f

    override fun analyze(detections: List<Detection>, trafficLights: List<TrafficLightObservation>): SceneState {
        val now = System.currentTimeMillis()

        val hazardCandidates = detections
            .filter { it.confidence >= confidenceThreshold }
            .mapNotNull { mapDetectionToHazard(it) }

        val primaryTl = trafficLights.firstOrNull()
        val tlHazard = primaryTl?.let {
            when (it.phase) {
                TrafficLightPhase.RED -> HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_RED,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.WARNING,
                    zone = ProximityZone.MID,
                    confidence = it.confidence
                )
                TrafficLightPhase.GREEN -> HazardEvent(
                    type = HazardType.TRAFFIC_LIGHT_GREEN,
                    direction = Direction.CENTER,
                    urgency = HazardLevel.NONE,
                    zone = ProximityZone.MID,
                    confidence = it.confidence
                )
                else -> null
            }
        }

        val combinedHazards = buildList {
            addAll(hazardCandidates)
            tlHazard?.let { add(it) }
        }

        val primaryHazard = selectPrimaryHazard(combinedHazards)

        if (combinedHazards.isEmpty()) {
            if (lastRelevantDetectionAt != 0L && now - lastRelevantDetectionAt > decayMillis) {
                lastRelevantDetectionAt = 0L
            }
            return SceneState(
                timestamp = now,
                detections = detections,
                hazards = emptyList(),
                primaryMessage = null,
                overallHazardLevel = HazardLevel.NONE,
                trafficLights = trafficLights,
                primaryTrafficLight = primaryTl?.phase,
                primaryHazard = primaryHazard
            )
        }

        lastRelevantDetectionAt = now
        val overallLevel = primaryHazard?.urgency ?: HazardLevel.NONE

        val primaryMessage = buildString {
            when (primaryTl?.phase) {
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
                else -> {
                    if (primaryHazard != null) {
                        append(hazardMessage(primaryHazard))
                    }
                }
            }
        }.ifEmpty { null }

        return SceneState(
            timestamp = now,
            detections = detections,
            hazards = combinedHazards,
            primaryMessage = primaryMessage,
            overallHazardLevel = overallLevel,
            trafficLights = trafficLights,
            primaryTrafficLight = primaryTl?.phase,
            primaryHazard = primaryHazard
        )
    }

    private fun mapDetectionToHazard(detection: Detection): HazardEvent? {
        val label = detection.label.lowercase()
        val type = when (label) {
            "person" -> HazardType.PERSON_AHEAD
            "car", "truck", "bus", "motorcycle", "bicycle" -> HazardType.VEHICLE_AHEAD
            in OBSTACLE_PROXY_LABELS -> HazardType.OBSTACLE_AHEAD
            else -> null
        } ?: return null
        val dir = directionFromBox(detection.bbox)
        val zone = zoneFromBox(detection.bbox)
        val (poleLike, aspectRatio) = poleLikeHeuristic(label, detection.bbox)
        val urgency = hazardLevelWithPole(zone, detection.confidence, poleLike)
        return HazardEvent(
            type = type,
            direction = dir,
            urgency = urgency,
            zone = zone,
            confidence = detection.confidence,
            poleLike = poleLike,
            aspectRatio = aspectRatio
        )
    }

    private fun selectPrimaryHazard(hazards: List<HazardEvent>): HazardEvent? {
        return hazards.sortedWith(
            compareByDescending<HazardEvent> { it.urgency }
                .thenByDescending { zoneScore(it.zone) }
                .thenByDescending { if (it.poleLike) 1 else 0 }
                .thenByDescending { it.confidence }
        ).firstOrNull()
    }

    private fun zoneScore(zone: ProximityZone): Int = when (zone) {
        ProximityZone.NEAR -> 3
        ProximityZone.MID -> 2
        ProximityZone.FAR -> 1
    }

    private fun hazardMessage(event: HazardEvent): String {
        val dirText = when (event.direction) {
            Direction.LEFT -> "links"
            Direction.RIGHT -> "rechts"
            Direction.CENTER, null -> "voraus"
        }
        return when (event.type) {
            HazardType.PERSON_AHEAD -> "Achtung, Person $dirText"
            HazardType.VEHICLE_AHEAD -> "Achtung, Fahrzeug $dirText"
            HazardType.OBSTACLE_AHEAD -> "Hindernis $dirText"
            HazardType.TRAFFIC_LIGHT_RED -> "Ampel rot"
            HazardType.TRAFFIC_LIGHT_GREEN -> "Ampel grün"
            HazardType.UNKNOWN -> "Warnung"
        }
    }

    private fun poleLikeHeuristic(label: String, bbox: com.example.bikeassist.ml.BoundingBox): Pair<Boolean, Float> {
        val width = (bbox.xMax - bbox.xMin).coerceAtLeast(1e-6f)
        val height = (bbox.yMax - bbox.yMin).coerceAtLeast(1e-6f)
        val ar = height / width
        val isPoleLabel = label in STRONG_OBSTACLE_LABELS
        val tall = ar >= 2.5f && height >= 0.20f
        val poleLike = isPoleLabel && tall
        return poleLike to ar
    }

    private fun hazardLevelWithPole(zone: ProximityZone, conf: Float, poleLike: Boolean): HazardLevel {
        val base = when (zone) {
            ProximityZone.NEAR -> if (conf > 0.6f) HazardLevel.DANGER else HazardLevel.WARNING
            ProximityZone.MID -> HazardLevel.WARNING
            ProximityZone.FAR -> HazardLevel.NONE
        }
        if (!poleLike) return base
        return when (base) {
            HazardLevel.DANGER -> HazardLevel.DANGER
            HazardLevel.WARNING -> HazardLevel.DANGER
            HazardLevel.NONE -> HazardLevel.WARNING
        }
    }
}
