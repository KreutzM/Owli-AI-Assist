package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection

data class SceneState(
    val timestamp: Long,
    val detections: List<Detection>,
    val hazards: List<HazardEvent>,
    val primaryMessage: String?,
    val overallHazardLevel: HazardLevel
)
