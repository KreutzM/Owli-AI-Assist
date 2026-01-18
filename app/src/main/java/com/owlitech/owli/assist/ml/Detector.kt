package com.owlitech.owli.assist.ml

import android.graphics.Bitmap
import java.io.Closeable

/**
 * Führt ML-Inferenz aus.
 */
interface Detector : Closeable {
    fun warmup()
    fun detect(input: Bitmap): List<Detection>
}
