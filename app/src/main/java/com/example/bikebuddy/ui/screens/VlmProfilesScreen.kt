package com.example.bikebuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bikeassist.vlm.VlmProfile
import com.example.bikebuddy.ui.components.SectionCard

@Composable
fun VlmProfilesScreen(
    profiles: List<VlmProfile>,
    activeProfileId: String,
    onSelect: (VlmProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(profiles, key = { it.id }) { profile ->
            val isActive = profile.id == activeProfileId
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(profile) }
            ) {
                SectionCard(
                    title = if (isActive) "${profile.label} (Aktiv)" else profile.label
                ) {
                    profile.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    val tempText = profile.parameterOverrides.temperature?.let { "%.2f".format(it) } ?: "n/a"
                    val effortText = profile.tokenPolicy.reasoningEffort?.let { "  Reasoning: $it" } ?: ""
                    Text(
                        text = "Model: ${profile.modelId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val streamingText = if (profile.streamingEnabled) "  Streaming: on" else "  Streaming: off"
                    Text(
                        text = "Temp: $tempText  MaxTokens: ${profile.tokenPolicy.maxTokens}$effortText$streamingText",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
