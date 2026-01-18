package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.blindview.AnnouncedObject
import android.graphics.Bitmap
import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.processing.FrameMapping

data class SceneState(
    val timestamp: Long,
    val detections: List<Detection>,
    val hazards: List<HazardEvent>,
    val primaryMessage: String?,
    val overallHazardLevel: HazardLevel,
    val trafficLights: List<TrafficLightObservation> = emptyList(),
    val primaryTrafficLight: TrafficLightPhase? = null,
    val blindViewItems: List<AnnouncedObject> = emptyList(),
    val blindViewUtterancePreview: String? = null,
    val frameMapping: FrameMapping? = null,
    val detectorDebugBitmap: Bitmap? = null
)
