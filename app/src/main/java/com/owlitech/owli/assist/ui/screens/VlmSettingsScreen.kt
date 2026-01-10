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
fun VlmSettingsScreen(
    settings: AppSettings,
    activeVlmProfileLabel: String,
    onOpenVlmProfiles: () -> Unit,
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
                Text("VLM Profil", style = MaterialTheme.typography.titleSmall)
                Text(activeVlmProfileLabel, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onOpenVlmProfiles) { Text("Auswaehlen") }
        }
        SettingSwitch(
            label = "Streaming TTS (VLM)",
            checked = settings.streamingVlmTtsEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(streamingVlmTtsEnabled = v) } },
            helper = "Frueher TTS-Start bei Streaming"
        )
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
