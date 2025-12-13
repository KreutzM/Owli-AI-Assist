package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection
import com.example.bikeassist.domain.TrafficLightObservation

interface SceneAnalyzer {
    fun analyze(detections: List<Detection>, trafficLights: List<TrafficLightObservation> = emptyList()): SceneState
}
