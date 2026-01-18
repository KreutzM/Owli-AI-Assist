package com.owlitech.owli.assist.processing

import android.graphics.Bitmap

data class PreprocessResult(
    val bitmap448: Bitmap,
    val mapping: FrameMapping?,
    val appliedRollDeg: Float
)
