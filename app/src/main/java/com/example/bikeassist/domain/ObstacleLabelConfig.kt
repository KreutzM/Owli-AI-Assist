package com.example.bikeassist.domain

val STRONG_OBSTACLE_LABELS = setOf(
    "fire hydrant",
    "parking meter",
    "stop sign"
)

val WEAK_OBSTACLE_LABELS = setOf(
    "bench",
    "chair",
    "potted plant"
)

val OBSTACLE_PROXY_LABELS = STRONG_OBSTACLE_LABELS + WEAK_OBSTACLE_LABELS
