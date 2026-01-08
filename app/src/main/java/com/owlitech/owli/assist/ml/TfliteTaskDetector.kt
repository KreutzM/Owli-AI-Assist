package com.owlitech.owli.assist.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection as TaskDetection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.Closeable

data class TfliteDetectorOptions(
    val modelPath: String = "models/efficientdet_lite2_int8.tflite",
    val numThreads: Int = 2,
    val useNnapi: Boolean = false,
    val maxResults: Int = 3,
    val scoreThreshold: Float = 0.3f
)

class TfliteTaskDetector(
    context: Context,
    private val options: TfliteDetectorOptions = TfliteDetectorOptions()
) : Detector, Closeable {

    private var detector: ObjectDetector? = null

    init {
        detector = buildDetector(context.applicationContext, options)
    }

    override fun warmup() {
        // Optional: kleiner Dummy-Run mit 1x1 Bitmap, falls nÃ¶tig
        val dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        detect(dummy)
    }

    override fun detect(input: Bitmap): List<Detection> {
        val localDetector = detector ?: return emptyList()
        val tensorImage = TensorImage.fromBitmap(input)
        val results = localDetector.detect(tensorImage)
        return results.mapNotNull { it.toDetection(input.width.toFloat(), input.height.toFloat()) }
    }

    override fun close() {
        detector?.close()
        detector = null
    }

    private fun buildDetector(context: Context, detectorOptions: TfliteDetectorOptions): ObjectDetector {
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(detectorOptions.numThreads)
        if (detectorOptions.useNnapi) {
            baseOptionsBuilder.useNnapi()
        }
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMaxResults(detectorOptions.maxResults)
            .setScoreThreshold(detectorOptions.scoreThreshold)
            .build()
        return ObjectDetector.createFromFileAndOptions(context, detectorOptions.modelPath, options)
    }

    private fun TaskDetection.toDetection(imageWidth: Float, imageHeight: Float): Detection? {
        val categories = categories
        val topCategory = categories.maxByOrNull { it.score } ?: return null
        val bbox = boundingBox
        val xMin = bbox.left / imageWidth
        val yMin = bbox.top / imageHeight
        val xMax = bbox.right / imageWidth
        val yMax = bbox.bottom / imageHeight
        return Detection(
            label = topCategory.label,
            confidence = topCategory.score,
            bbox = BoundingBox(
                xMin = xMin.coerceIn(0f, 1f),
                yMin = yMin.coerceIn(0f, 1f),
                xMax = xMax.coerceIn(0f, 1f),
                yMax = yMax.coerceIn(0f, 1f)
            )
        )
    }
}
