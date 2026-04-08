package com.owlitech.owli.assist.camera

import androidx.camera.core.ImageProxy

/**
 * Liefert Kamera-Frames an die Pipeline.
 *
 * Ownership contract: the caller that invokes [onFrame] remains responsible for
 * closing the [ImageProxy]. Implementations may read/process the frame but must
 * not close it themselves.
 */
interface FrameListener {
    fun onFrame(image: ImageProxy)
}
