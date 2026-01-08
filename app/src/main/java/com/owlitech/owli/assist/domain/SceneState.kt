package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.blindview.AnnouncedObject
import com.owlitech.owli.assist.ml.Detection

data class SceneState(
    val timestamp: Long,
    val detections: List<Detection>,
    val hazards: List<HazardEvent>,
    val primaryMessage: String?,
    val overallHazardLevel: HazardLevel,
    val trafficLights: List<TrafficLightObservation> = emptyList(),
    val primaryTrafficLight: TrafficLightPhase? = null,
    val blindViewItems: List<AnnouncedObject> = emptyList(),
    val blindViewUtterancePreview: String? = null
)
