package com.owlitech.owli.assist.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.owlitech.owli.assist.util.AppLogger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class MotionEstimator(
    context: Context,
    medThresholdRadS: Float = DEFAULT_MED_THRESHOLD_RAD_S,
    highThresholdRadS: Float = DEFAULT_HIGH_THRESHOLD_RAD_S,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationSamples = Array<RotationSample?>(bufferSize) { null }
    private val gyroSamples = Array<GyroSample?>(bufferSize) { null }
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val lock = Any()
    private var rotationIndex = 0
    private var gyroIndex = 0
    private var running = false
    private var lastDebugLogNs = 0L
    private var medThresholdRadS = medThresholdRadS
    private var highThresholdRadS = highThresholdRadS

    fun start(): Boolean {
        if (running) return true
        if (rotationSensor == null && gyroSensor == null) {
            AppLogger.w(TAG, "Motion sensors unavailable")
            return false
        }
        rotationSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        running = true
        AppLogger.d(TAG, "MotionEstimator started (rotation=${rotationSensor != null}, gyro=${gyroSensor != null})")
        return true
    }

    fun stop() {
        if (!running) return
        sensorManager.unregisterListener(this)
        running = false
        AppLogger.d(TAG, "MotionEstimator stopped")
    }

    fun updateThresholds(medThresholdRadS: Float, highThresholdRadS: Float) {
        val safeMed = medThresholdRadS.coerceAtLeast(0f)
        val safeHigh = highThresholdRadS.coerceAtLeast(safeMed)
        this.medThresholdRadS = safeMed
        this.highThresholdRadS = safeHigh
    }

    fun getSnapshot(timestampNs: Long): MotionSnapshot? {
        val rotationSample: RotationSample?
        val gyroSample: GyroSample?
        synchronized(lock) {
            rotationSample = findClosestRotation(timestampNs)
            gyroSample = findClosestGyro(timestampNs)
        }
        if (rotationSample == null && gyroSample == null) return null
        val gyroMag = gyroSample?.magRadS ?: 0f
        val motionLevel = computeMotionLevel(gyroMag)
        val roll = rotationSample?.rollRad ?: 0f
        val pitch = rotationSample?.pitchRad ?: 0f
        val yaw = rotationSample?.yawRad
        val quality = computeQuality(timestampNs, rotationSample?.timestampNs, gyroSample?.timestampNs)
        maybeLogDebug(timestampNs, motionLevel, gyroMag, roll, pitch, quality)
        return MotionSnapshot(
            timestampNs = timestampNs,
            rollRad = roll,
            pitchRad = pitch,
            yawRad = yaw,
            gyroMagRadS = gyroMag,
            motionLevel = motionLevel,
            quality = quality
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> handleRotation(event)
            Sensor.TYPE_GYROSCOPE -> handleGyro(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun handleRotation(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val yaw = orientation[0]
        val pitch = orientation[1]
        val roll = orientation[2]
        synchronized(lock) {
            rotationSamples[rotationIndex] = RotationSample(event.timestamp, roll, pitch, yaw)
            rotationIndex = (rotationIndex + 1) % bufferSize
        }
    }

    private fun handleGyro(event: SensorEvent) {
        val x = event.values.getOrNull(0) ?: 0f
        val y = event.values.getOrNull(1) ?: 0f
        val z = event.values.getOrNull(2) ?: 0f
        val mag = sqrt(x * x + y * y + z * z)
        synchronized(lock) {
            gyroSamples[gyroIndex] = GyroSample(event.timestamp, mag)
            gyroIndex = (gyroIndex + 1) % bufferSize
        }
    }

    private fun findClosestRotation(targetNs: Long): RotationSample? {
        var best: RotationSample? = null
        var bestDelta = Long.MAX_VALUE
        rotationSamples.forEach { sample ->
            if (sample == null) return@forEach
            val delta = abs(targetNs - sample.timestampNs)
            if (delta < bestDelta) {
                bestDelta = delta
                best = sample
            }
        }
        return best
    }

    private fun findClosestGyro(targetNs: Long): GyroSample? {
        var best: GyroSample? = null
        var bestDelta = Long.MAX_VALUE
        gyroSamples.forEach { sample ->
            if (sample == null) return@forEach
            val delta = abs(targetNs - sample.timestampNs)
            if (delta < bestDelta) {
                bestDelta = delta
                best = sample
            }
        }
        return best
    }

    private fun computeMotionLevel(gyroMagRadS: Float): MotionLevel {
        return when {
            gyroMagRadS >= highThresholdRadS -> MotionLevel.HIGH
            gyroMagRadS >= medThresholdRadS -> MotionLevel.MED
            else -> MotionLevel.LOW
        }
    }

    private fun computeQuality(targetNs: Long, rotationNs: Long?, gyroNs: Long?): Float {
        val rotationQuality = qualityForSample(targetNs, rotationNs)
        val gyroQuality = qualityForSample(targetNs, gyroNs)
        return if (rotationQuality > 0f && gyroQuality > 0f) {
            (rotationQuality + gyroQuality) * 0.5f
        } else {
            max(rotationQuality, gyroQuality)
        }
    }

    private fun qualityForSample(targetNs: Long, sampleNs: Long?): Float {
        if (sampleNs == null) return 0f
        val delta = abs(targetNs - sampleNs).coerceAtMost(QUALITY_MAX_AGE_NS)
        return (1f - delta.toFloat() / QUALITY_MAX_AGE_NS.toFloat()).coerceIn(0f, 1f)
    }

    private fun maybeLogDebug(
        timestampNs: Long,
        motionLevel: MotionLevel,
        gyroMag: Float,
        roll: Float,
        pitch: Float,
        quality: Float
    ) {
        if (timestampNs - lastDebugLogNs < DEBUG_LOG_INTERVAL_NS) return
        lastDebugLogNs = timestampNs
        AppLogger.d(
            TAG,
            "Motion level=$motionLevel gyro=${"%.2f".format(gyroMag)} roll=${"%.2f".format(roll)} pitch=${"%.2f".format(pitch)} q=${"%.2f".format(quality)}"
        )
    }

    private data class RotationSample(
        val timestampNs: Long,
        val rollRad: Float,
        val pitchRad: Float,
        val yawRad: Float
    )

    private data class GyroSample(
        val timestampNs: Long,
        val magRadS: Float
    )

    companion object {
        private const val TAG = "MotionEstimator"
        private const val DEFAULT_BUFFER_SIZE = 120
        private const val DEFAULT_MED_THRESHOLD_RAD_S = 0.9f
        private const val DEFAULT_HIGH_THRESHOLD_RAD_S = 1.6f
        private const val DEBUG_LOG_INTERVAL_NS = 2_000_000_000L
        private const val QUALITY_MAX_AGE_NS = 400_000_000L
    }
}
