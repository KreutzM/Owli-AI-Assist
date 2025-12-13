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

data class HazardEvent(
    val type: HazardType,
    val direction: Direction? = null,
    val urgency: HazardLevel = HazardLevel.NONE
)
