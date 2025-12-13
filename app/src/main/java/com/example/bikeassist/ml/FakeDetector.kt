package com.example.bikeassist.ml

/**
 * Fake-Detector, der immer eine Beispiel-Person liefert.
 */
class FakeDetector : Detector {
    override fun warmup() {
        // Kein Warmup nötig.
    }

    override fun detect(input: FloatArray): List<Detection> {
        val bbox = BoundingBox(
            xMin = 0.35f,
            yMin = 0.45f,
            xMax = 0.65f,
            yMax = 0.85f
        )
        return listOf(
            Detection(
                label = "person",
                confidence = 0.9f,
                bbox = bbox
            )
        )
    }

    override fun close() {
        // Keine Ressourcen zu schließen.
    }
}
