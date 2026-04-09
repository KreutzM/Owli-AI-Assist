package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.settings.LanguagePreference
import com.owlitech.owli.assist.settings.OpenRouterKeyMode

@Composable
fun VlmSettingsScreen(
    settings: AppSettings,
    activeVlmProfileLabel: String,
    hasOpenRouterUserKey: Boolean,
    onOpenVlmProfiles: () -> Unit,
    onOpenQrImport: () -> Unit,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit,
    onSaveOpenRouterUserKey: (String) -> Unit,
    onClearOpenRouterUserKey: () -> Unit
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
        OpenRouterKeySetting(
            keyMode = settings.openRouterKeyMode,
            hasStoredKey = hasOpenRouterUserKey,
            onSelectMode = { mode -> onUpdate { it.copy(openRouterKeyMode = mode) } },
            onOpenQrImport = onOpenQrImport,
            onSaveKey = onSaveOpenRouterUserKey,
            onClearKey = onClearOpenRouterUserKey
        )
        SettingSwitch(
            label = stringResource(R.string.settings_tts_enabled),
            checked = settings.ttsEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(ttsEnabled = v) } },
            helper = stringResource(R.string.settings_tts_enabled_helper)
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
    }
}

@Composable
private fun OpenRouterKeySetting(
    keyMode: OpenRouterKeyMode,
    hasStoredKey: Boolean,
    onSelectMode: (OpenRouterKeyMode) -> Unit,
    onOpenQrImport: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit
) {
    var draftKey by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var localStatus by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val activeStatus = when (keyMode) {
        OpenRouterKeyMode.EMBEDDED_APP_KEY -> stringResource(R.string.vlm_settings_openrouter_key_status_embedded)
        OpenRouterKeyMode.USER_PROVIDED_KEY -> stringResource(R.string.vlm_settings_openrouter_key_status_user)
    }
    val storedStatus = if (hasStoredKey) {
        stringResource(R.string.vlm_settings_openrouter_key_stored_yes)
    } else {
        stringResource(R.string.vlm_settings_openrouter_key_stored_no)
    }
    val keySavedStatus = stringResource(R.string.vlm_settings_openrouter_key_saved)
    val keyClearedStatus = stringResource(R.string.vlm_settings_openrouter_key_cleared)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.vlm_settings_openrouter_key_title), style = MaterialTheme.typography.titleSmall)
        Text(activeStatus, style = MaterialTheme.typography.bodyMedium)
        Text(storedStatus, style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.vlm_settings_openrouter_key_helper), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = draftKey,
            onValueChange = {
                draftKey = it
                localStatus = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.vlm_settings_openrouter_key_input_label)) },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                showKey = !showKey
            }) {
                Text(
                    if (showKey) {
                        stringResource(R.string.vlm_settings_openrouter_key_hide)
                    } else {
                        stringResource(R.string.vlm_settings_openrouter_key_show)
                    }
                )
            }
            TextButton(onClick = {
                clipboardManager.getText()?.text?.let {
                    draftKey = it
                    localStatus = null
                }
            }) {
                Text(stringResource(R.string.vlm_settings_openrouter_key_paste))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSaveKey(draftKey)
                    draftKey = ""
                    showKey = false
                    localStatus = keySavedStatus
                },
                enabled = draftKey.isNotBlank()
            ) {
                Text(stringResource(R.string.vlm_settings_openrouter_key_save))
            }
            OutlinedButton(onClick = {
                onSelectMode(OpenRouterKeyMode.EMBEDDED_APP_KEY)
            }) {
                Text(stringResource(R.string.vlm_settings_openrouter_key_use_app_key))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenQrImport) {
                Text(stringResource(R.string.vlm_settings_openrouter_key_scan_qr))
            }
            OutlinedButton(
                onClick = {
                    onSelectMode(OpenRouterKeyMode.USER_PROVIDED_KEY)
                },
                enabled = hasStoredKey
            ) {
                Text(stringResource(R.string.vlm_settings_openrouter_key_use_custom_key))
            }
        }
        OutlinedButton(
            onClick = {
                onClearKey()
                draftKey = ""
                showKey = false
                localStatus = keyClearedStatus
            },
            enabled = hasStoredKey
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_clear))
        }
        localStatus?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        if (!hasStoredKey && keyMode == OpenRouterKeyMode.USER_PROVIDED_KEY) {
            Text(
                stringResource(R.string.vlm_settings_openrouter_key_missing_user_key),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (keyMode == OpenRouterKeyMode.EMBEDDED_APP_KEY && hasStoredKey) {
            Text(
                stringResource(R.string.vlm_settings_openrouter_key_stored_but_app_active),
                style = MaterialTheme.typography.bodySmall
            )
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
