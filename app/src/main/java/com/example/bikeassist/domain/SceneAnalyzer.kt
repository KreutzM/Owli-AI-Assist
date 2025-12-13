package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection

interface SceneAnalyzer {
    fun analyze(detections: List<Detection>): SceneState
}
