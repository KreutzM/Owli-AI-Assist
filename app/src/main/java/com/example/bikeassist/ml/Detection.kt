package com.example.bikeassist.ml

data class Detection(
    val label: String,
    val confidence: Float,
    val bbox: BoundingBox
)
