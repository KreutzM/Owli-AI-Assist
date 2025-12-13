package com.example.bikeassist.ml

/**
 * Normalisierte Bounding Box (0..1) im Bildkoordinatensystem.
 */
data class BoundingBox(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)
