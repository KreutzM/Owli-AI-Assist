package com.example.bikeassist.domain

enum class HazardLevel { NONE, WARNING, DANGER }

enum class HazardType {
    PERSON_AHEAD,
    VEHICLE_AHEAD,
    OBSTACLE_AHEAD,
    TRAFFIC_LIGHT_RED,
    TRAFFIC_LIGHT_GREEN,
    UNKNOWN
}

enum class Direction { LEFT, RIGHT, CENTER }

enum class ProximityZone { FAR, MID, NEAR }

data class HazardEvent(
    val type: HazardType,
    val direction: Direction? = null,
    val urgency: HazardLevel = HazardLevel.NONE,
    val zone: ProximityZone = ProximityZone.FAR,
    val confidence: Float = 0f,
    val poleLike: Boolean = false,
    val aspectRatio: Float? = null
)
