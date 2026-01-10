package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.settings.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onReset) { Text("Reset") }
        }
        SettingSlider(
            label = "Detector minConfidence",
            value = settings.detectorMinConfidence,
            valueRange = 0.05f..0.95f,
            steps = 9,
            onValueChange = { v -> onUpdate { it.copy(detectorMinConfidence = v) } },
            helper = "Score-Threshold"
        )
        SettingIntSlider(
            label = "Detector maxResults",
            value = settings.detectorMaxResults,
            valueRange = 1..10,
            onValueChange = { v -> onUpdate { it.copy(detectorMaxResults = v) } },
            helper = "Top-N Ergebnisse"
        )
        SettingIntSlider(
            label = "Threads",
            value = settings.detectorNumThreads,
            valueRange = 1..4,
            onValueChange = { v -> onUpdate { it.copy(detectorNumThreads = v) } },
            helper = "Interpreter Threads"
        )
        SettingSwitch(
            label = "NNAPI",
            checked = settings.detectorUseNnapi,
            onCheckedChange = { v -> onUpdate { it.copy(detectorUseNnapi = v) } },
            helper = "Beschleuniger (wenn verfuegbar)"
        )
        SettingSlider(
            label = "Track minConfidence",
            value = settings.minConfidenceTrack,
            valueRange = 0.1f..0.95f,
            steps = 8,
            onValueChange = { v -> onUpdate { it.copy(minConfidenceTrack = v) } },
            helper = "Filter gegen False Positives"
        )
        SettingSlider(
            label = "IoU Threshold",
            value = settings.iouThreshold,
            valueRange = 0.1f..0.9f,
            steps = 8,
            onValueChange = { v -> onUpdate { it.copy(iouThreshold = v) } },
            helper = "Matching-Schwelle Tracker"
        )
        SettingIntSlider(
            label = "Min Hits",
            value = settings.minConsecutiveHits,
            valueRange = 1..5,
            onValueChange = { v -> onUpdate { it.copy(minConsecutiveHits = v) } },
            helper = "Stabilitaet Tracker"
        )
        SettingSlider(
            label = "BBox Glaettung",
            value = settings.bboxSmoothingAlpha,
            valueRange = 0f..1f,
            steps = 9,
            onValueChange = { v -> onUpdate { it.copy(bboxSmoothingAlpha = v) } },
            helper = "EMA-Anteil (hoeher = glatter)"
        )
        SettingIntSlider(
            label = "BlindView max Items",
            value = settings.maxItemsSpoken,
            valueRange = 1..12,
            onValueChange = { v -> onUpdate { it.copy(maxItemsSpoken = v) } },
            helper = "Anzahl Objekte pro Ansage"
        )
        SettingSlider(
            label = "TTS Speech Rate",
            value = settings.ttsSpeechRate,
            valueRange = 0.5f..3.0f,
            steps = 10,
            onValueChange = { v -> onUpdate { it.copy(ttsSpeechRate = v) } },
            helper = "Sprechgeschwindigkeit"
        )
        SettingSlider(
            label = "TTS Pitch",
            value = settings.ttsPitch,
            valueRange = 0.5f..2.0f,
            steps = 6,
            onValueChange = { v -> onUpdate { it.copy(ttsPitch = v) } },
            helper = "Stimmhoehe"
        )
        SettingIntSlider(
            label = "Analysis Interval (ms)",
            value = settings.analysisIntervalMs.toInt(),
            valueRange = 150..1000,
            onValueChange = { v -> onUpdate { it.copy(analysisIntervalMs = v.toLong()) } },
            helper = "Min Abstand zwischen Frames"
        )
        SettingSwitch(
            label = "Overlay anzeigen",
            checked = settings.showOverlay,
            onCheckedChange = { v -> onUpdate { it.copy(showOverlay = v) } }
        )
        SettingSwitch(
            label = "Overlay Labels",
            checked = settings.showOverlayLabels,
            onCheckedChange = { v -> onUpdate { it.copy(showOverlayLabels = v) } },
            helper = "BBox-Beschriftung (Label + Confidence)"
        )
        SettingSwitch(
            label = "BlindView Preview",
            checked = settings.showBlindViewPreview,
            onCheckedChange = { v -> onUpdate { it.copy(showBlindViewPreview = v) } }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onReset) { Text("Reset Defaults") }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    helper: String? = null
) {
    Column {
        Text(text = "$label: ${"%.2f".format(value)}")
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
        helper?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SettingIntSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    helper: String? = null
) {
    SettingSlider(
        label = label,
        value = value.toFloat(),
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        steps = (valueRange.last - valueRange.first),
        onValueChange = { onValueChange(it.toInt()) },
        helper = helper
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    helper: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label)
            helper?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
