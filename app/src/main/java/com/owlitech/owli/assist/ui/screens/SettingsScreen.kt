package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.settings.LanguagePreference

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
            Button(onClick = onReset) { Text(stringResource(R.string.settings_reset)) }
        }
        LanguageSetting(
            selected = settings.languagePreference,
            onSelected = { pref -> onUpdate { it.copy(languagePreference = pref) } }
        )
        SettingSlider(
            label = stringResource(R.string.settings_detector_min_confidence),
            value = settings.detectorMinConfidence,
            valueRange = 0.05f..0.95f,
            steps = 9,
            onValueChange = { v -> onUpdate { it.copy(detectorMinConfidence = v) } },
            helper = stringResource(R.string.settings_detector_min_confidence_helper)
        )
        SettingIntSlider(
            label = stringResource(R.string.settings_detector_max_results),
            value = settings.detectorMaxResults,
            valueRange = 1..10,
            onValueChange = { v -> onUpdate { it.copy(detectorMaxResults = v) } },
            helper = stringResource(R.string.settings_detector_max_results_helper)
        )
        SettingIntSlider(
            label = stringResource(R.string.settings_detector_threads),
            value = settings.detectorNumThreads,
            valueRange = 1..4,
            onValueChange = { v -> onUpdate { it.copy(detectorNumThreads = v) } },
            helper = stringResource(R.string.settings_detector_threads_helper)
        )
        SettingSwitch(
            label = stringResource(R.string.settings_detector_nnapi),
            checked = settings.detectorUseNnapi,
            onCheckedChange = { v -> onUpdate { it.copy(detectorUseNnapi = v) } },
            helper = stringResource(R.string.settings_detector_nnapi_helper)
        )
        SettingSlider(
            label = stringResource(R.string.settings_track_min_confidence),
            value = settings.minConfidenceTrack,
            valueRange = 0.1f..0.95f,
            steps = 8,
            onValueChange = { v -> onUpdate { it.copy(minConfidenceTrack = v) } },
            helper = stringResource(R.string.settings_track_min_confidence_helper)
        )
        SettingSlider(
            label = stringResource(R.string.settings_iou_threshold),
            value = settings.iouThreshold,
            valueRange = 0.1f..0.9f,
            steps = 8,
            onValueChange = { v -> onUpdate { it.copy(iouThreshold = v) } },
            helper = stringResource(R.string.settings_iou_threshold_helper)
        )
        SettingIntSlider(
            label = stringResource(R.string.settings_min_hits),
            value = settings.minConsecutiveHits,
            valueRange = 1..5,
            onValueChange = { v -> onUpdate { it.copy(minConsecutiveHits = v) } },
            helper = stringResource(R.string.settings_min_hits_helper)
        )
        SettingSlider(
            label = stringResource(R.string.settings_bbox_smoothing),
            value = settings.bboxSmoothingAlpha,
            valueRange = 0f..1f,
            steps = 9,
            onValueChange = { v -> onUpdate { it.copy(bboxSmoothingAlpha = v) } },
            helper = stringResource(R.string.settings_bbox_smoothing_helper)
        )
        SettingIntSlider(
            label = stringResource(R.string.settings_blindview_max_items),
            value = settings.maxItemsSpoken,
            valueRange = 1..12,
            onValueChange = { v -> onUpdate { it.copy(maxItemsSpoken = v) } },
            helper = stringResource(R.string.settings_blindview_max_items_helper)
        )
        SettingSlider(
            label = stringResource(R.string.settings_tts_speech_rate),
            value = settings.ttsSpeechRate,
            valueRange = 0.5f..3.0f,
            steps = 10,
            onValueChange = { v -> onUpdate { it.copy(ttsSpeechRate = v) } },
            helper = stringResource(R.string.settings_tts_speech_rate_helper)
        )
        SettingSlider(
            label = stringResource(R.string.settings_tts_pitch),
            value = settings.ttsPitch,
            valueRange = 0.5f..2.0f,
            steps = 6,
            onValueChange = { v -> onUpdate { it.copy(ttsPitch = v) } },
            helper = stringResource(R.string.settings_tts_pitch_helper)
        )
        SettingIntSlider(
            label = stringResource(R.string.settings_analysis_interval),
            value = settings.analysisIntervalMs.toInt(),
            valueRange = 150..1000,
            onValueChange = { v -> onUpdate { it.copy(analysisIntervalMs = v.toLong()) } },
            helper = stringResource(R.string.settings_analysis_interval_helper)
        )
        SettingSwitch(
            label = stringResource(R.string.settings_show_overlay),
            checked = settings.showOverlay,
            onCheckedChange = { v -> onUpdate { it.copy(showOverlay = v) } }
        )
        SettingSwitch(
            label = stringResource(R.string.settings_show_overlay_labels),
            checked = settings.showOverlayLabels,
            onCheckedChange = { v -> onUpdate { it.copy(showOverlayLabels = v) } },
            helper = stringResource(R.string.settings_show_overlay_labels_helper)
        )
        SettingSwitch(
            label = stringResource(R.string.settings_blindview_preview),
            checked = settings.showBlindViewPreview,
            onCheckedChange = { v -> onUpdate { it.copy(showBlindViewPreview = v) } }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onReset) { Text(stringResource(R.string.settings_reset_defaults)) }
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
        Text(text = stringResource(R.string.settings_slider_value_format, label, value))
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

@Composable
private fun LanguageSetting(
    selected: LanguagePreference,
    onSelected: (LanguagePreference) -> Unit
) {
    val options = listOf(
        LanguagePreference.SYSTEM to stringResource(R.string.settings_language_system),
        LanguagePreference.DE to stringResource(R.string.settings_language_german),
        LanguagePreference.EN to stringResource(R.string.settings_language_english)
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_language_hint), style = MaterialTheme.typography.bodySmall)
        options.forEach { (option, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option == selected,
                        onClick = { onSelected(option) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
