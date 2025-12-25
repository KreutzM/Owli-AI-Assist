package com.example.bikeassist.pipeline

/**
 * Liefert einen Snapshot des zuletzt verarbeiteten Frames als JPEG.
 */
interface SnapshotProvider {
    fun getLatestJpegSnapshot(maxSidePx: Int = 1024, quality: Int = 80): ByteArray?

    suspend fun requestFreshJpegSnapshot(
        maxSidePx: Int = 1024,
        quality: Int = 80,
        timeoutMs: Long = 1500L
    ): ByteArray?
}
