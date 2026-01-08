package com.owlitech.owli.assist.camera

import androidx.camera.core.ImageProxy

/**
 * Liefert Kamera-Frames an die Pipeline.
 */
interface FrameListener {
    fun onFrame(image: ImageProxy)
}
