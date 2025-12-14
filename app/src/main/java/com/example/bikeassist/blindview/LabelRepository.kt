package com.example.bikeassist.blindview

import android.content.Context
import com.example.bikeassist.util.AppLogger

class LabelRepository {
    fun loadLabels(context: Context, assetPath: String = "models/labels.txt"): List<String> {
        return runCatching {
            context.assets.open(assetPath).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        }.onSuccess { labels ->
            if (labels.size != 80) {
                AppLogger.e(message = "labels.txt expected 80 labels, got ${labels.size}")
            }
        }.onFailure { ex ->
            AppLogger.e(ex, "Failed to load labels from $assetPath")
        }.getOrDefault(emptyList())
    }
}
