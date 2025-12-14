package com.example.bikeassist.blindview

enum class DistanceBin { NEAR, MID, FAR }

data class AnnouncedObject(
    val labelEn: String,
    val labelDeSingular: String,
    val labelDePlural: String,
    val count: Int,
    val clock: Int,
    val distance: DistanceBin,
    val centerX: Float,
    val maxConfidence: Float
)
