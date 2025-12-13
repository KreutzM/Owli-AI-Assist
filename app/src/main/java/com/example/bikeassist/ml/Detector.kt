package com.example.bikeassist.ml

import java.io.Closeable

/**
 * Führt ML-Inferenz aus.
 */
interface Detector : Closeable {
    fun warmup()
    fun detect(input: FloatArray): List<Detection>
}
