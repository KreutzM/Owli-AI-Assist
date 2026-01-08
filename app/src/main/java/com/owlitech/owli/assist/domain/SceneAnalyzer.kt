package com.owlitech.owli.assist.domain

import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.domain.TrafficLightObservation

interface SceneAnalyzer {
    fun analyze(detections: List<Detection>, trafficLights: List<TrafficLightObservation> = emptyList()): SceneState
}
