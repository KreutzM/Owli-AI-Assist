package com.owlitech.owli.assist.processing

import android.graphics.Bitmap

data class PreprocessResult(
    val bitmap448: Bitmap,
    val mapping: FrameMapping?,
    val appliedRollDeg: Float,
    val translationDxLowRes: Int,
    val translationDyLowRes: Int,
    val translationQuality: Float,
    val cropLeftPx: Int,
    val cropTopPx: Int,
    val patchCenterXLowRes: Int?,
    val patchCenterYLowRes: Int?
)
