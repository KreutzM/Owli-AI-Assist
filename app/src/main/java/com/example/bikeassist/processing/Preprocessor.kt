package com.example.bikeassist.processing

import androidx.camera.core.ImageProxy

/**
 * Wandelt Kamera-Frames in Modell-Input um.
 */
interface Preprocessor {
    fun preprocess(image: ImageProxy): FloatArray
}
