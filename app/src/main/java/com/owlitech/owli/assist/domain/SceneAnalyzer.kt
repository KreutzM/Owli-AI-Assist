package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.domain.TrafficLightObservation
import com.owlitech.owli.assist.motion.MotionSnapshot

interface SceneAnalyzer {
    fun analyze(
        detections: List<Detection>,
        trafficLights: List<TrafficLightObservation> = emptyList(),
        motion: MotionSnapshot? = null
    ): SceneState
}
