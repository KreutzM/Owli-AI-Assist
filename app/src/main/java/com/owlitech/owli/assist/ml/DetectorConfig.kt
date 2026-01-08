package com.owlitech.owli.assist.ml

data class DetectorConfig(
    val modelSpec: ModelSpec,
    val backend: Backend
)

enum class Backend {
    TFLITE,
    ONNX,
    MEDIAPIPE
}
