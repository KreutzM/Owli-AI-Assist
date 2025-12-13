package com.example.bikeassist.domain

import com.example.bikeassist.ml.Detection

/**
 * Platzhalter-Heuristik. Liefert einen leeren SceneState, bis echte Logik implementiert ist.
 */
class DefaultSceneAnalyzer : SceneAnalyzer {
    override fun analyze(detections: List<Detection>): SceneState {
        // TODO: Reale Hazard-Heuristiken implementieren.
        return SceneState(
            timestamp = System.currentTimeMillis(),
            detections = detections,
            hazards = emptyList(),
            primaryMessage = null,
            overallHazardLevel = HazardLevel.NONE
        )
    }
}
