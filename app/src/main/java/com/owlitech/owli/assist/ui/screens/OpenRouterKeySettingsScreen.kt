package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.settings.OpenRouterKeyMode

@Composable
fun OpenRouterKeySettingsScreen(
    keyMode: OpenRouterKeyMode,
    hasStoredKey: Boolean,
    onSelectMode: (OpenRouterKeyMode) -> Unit,
    onOpenQrImport: () -> Unit,
    onOpenKeyInfo: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit
) {
    var draftKey by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var localStatus by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val keySavedStatus = stringResource(R.string.vlm_settings_openrouter_key_saved)
    val keyClearedStatus = stringResource(R.string.vlm_settings_openrouter_key_cleared)
    val activeStatus = when (keyMode) {
        OpenRouterKeyMode.EMBEDDED_APP_KEY -> stringResource(R.string.vlm_settings_openrouter_key_status_embedded)
        OpenRouterKeyMode.USER_PROVIDED_KEY -> stringResource(R.string.vlm_settings_openrouter_key_status_user)
    }
    val storedStatus = if (hasStoredKey) {
        stringResource(R.string.vlm_settings_openrouter_key_stored_yes)
    } else {
        stringResource(R.string.vlm_settings_openrouter_key_stored_no)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.vlm_settings_openrouter_key_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.vlm_settings_openrouter_key_helper),
            style = MaterialTheme.typography.bodySmall
        )
        Text(text = activeStatus, style = MaterialTheme.typography.bodyMedium)
        Text(text = storedStatus, style = MaterialTheme.typography.bodySmall)
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
            TextButton(onClick = { showKey = !showKey }) {
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
        Button(
            onClick = {
                onSaveKey(draftKey)
                draftKey = ""
                showKey = false
                localStatus = keySavedStatus
            },
            enabled = draftKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_save))
        }
        OutlinedButton(
            onClick = { onSelectMode(OpenRouterKeyMode.EMBEDDED_APP_KEY) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_use_app_key))
        }
        OutlinedButton(
            onClick = { onSelectMode(OpenRouterKeyMode.USER_PROVIDED_KEY) },
            enabled = hasStoredKey,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_use_custom_key))
        }
        OutlinedButton(
            onClick = onOpenQrImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_scan_qr))
        }
        OutlinedButton(
            onClick = onOpenKeyInfo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vlm_settings_openrouter_key_info))
        }
        OutlinedButton(
            onClick = {
                onClearKey()
                draftKey = ""
                showKey = false
                localStatus = keyClearedStatus
            },
            enabled = hasStoredKey,
            modifier = Modifier.fillMaxWidth()
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
