package com.owlitech.owli.assist.ml

import com.owlitech.owli.assist.processing.ModelInputSpec

data class ModelSpec(
    val modelPath: String,
    val inputSpec: ModelInputSpec,
    val labels: List<String>,
    val outputShape: IntArray,
    val scoreThreshold: Float = 0.3f,
    val nmsThreshold: Float = 0.5f
)
