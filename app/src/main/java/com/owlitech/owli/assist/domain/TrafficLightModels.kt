package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.ml.BoundingBox

enum class TrafficLightPhase { RED, GREEN, UNKNOWN }

data class TrafficLightObservation(
    val bbox: BoundingBox,
    val phase: TrafficLightPhase,
    val confidence: Float,
    val redTop: Float? = null,
    val greenBottom: Float? = null
)
