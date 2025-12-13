package com.example.bikeassist.ml

interface DetectorFactory {
    fun create(config: DetectorConfig): Detector
}
