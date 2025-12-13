package com.example.bikeassist.camera

import androidx.camera.core.ImageProxy

/**
 * Liefert Kamera-Frames an die Pipeline.
 */
interface FrameListener {
    fun onFrame(image: ImageProxy)
}
