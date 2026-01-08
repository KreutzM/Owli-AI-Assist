package com.owlitech.owli.assist.ml

data class Detection(
    val label: String,
    val confidence: Float,
    val bbox: BoundingBox
)
