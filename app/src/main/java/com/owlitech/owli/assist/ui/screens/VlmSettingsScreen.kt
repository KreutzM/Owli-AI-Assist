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
fun VlmSettingsScreen(
    settings: AppSettings,
    activeVlmProfileLabel: String,
    onOpenVlmProfiles: () -> Unit,
    onOpenDetectorSettings: () -> Unit,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.vlm_settings_profile_title), style = MaterialTheme.typography.titleSmall)
                Text(activeVlmProfileLabel, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onOpenVlmProfiles) { Text(stringResource(R.string.vlm_settings_select)) }
        }
        SettingSwitch(
            label = stringResource(R.string.vlm_settings_streaming_tts),
            checked = settings.streamingVlmTtsEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(streamingVlmTtsEnabled = v) } },
            helper = stringResource(R.string.vlm_settings_streaming_tts_helper)
        )
        LanguageSetting(
            selected = settings.languagePreference,
            onSelected = { pref -> onUpdate { it.copy(languagePreference = pref) } }
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
        Text(
            stringResource(R.string.settings_section_developer),
            style = MaterialTheme.typography.titleSmall
        )
        SettingSwitch(
            label = stringResource(R.string.settings_detector_enable_experimental),
            checked = settings.detectorModeEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(detectorModeEnabled = v) } }
        )
        if (settings.detectorModeEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onOpenDetectorSettings) {
                    Text(stringResource(R.string.settings_open_detector_settings))
                }
            }
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
