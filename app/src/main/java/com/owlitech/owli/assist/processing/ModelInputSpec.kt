package com.owlitech.owli.assist.processing

data class ModelInputSpec(
    val width: Int,
    val height: Int,
    val channels: Int = 3,
    val normalizeMean: FloatArray,
    val normalizeStd: FloatArray
)
