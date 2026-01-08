package com.owlitech.owli.assist.ml

interface DetectorFactory {
    fun create(config: DetectorConfig): Detector
}
