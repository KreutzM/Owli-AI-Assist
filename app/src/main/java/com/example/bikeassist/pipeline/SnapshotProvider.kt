package com.example.bikeassist.pipeline

/**
 * Liefert einen Snapshot des zuletzt verarbeiteten Frames als JPEG.
 */
interface SnapshotProvider {
    fun getLatestJpegSnapshot(maxSidePx: Int = 1024, quality: Int = 80): ByteArray?
}
