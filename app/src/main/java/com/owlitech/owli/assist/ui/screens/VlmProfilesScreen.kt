package com.owlitech.owli.assist.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.vlm.VlmProfile
import com.owlitech.owli.assist.ui.components.SectionCard

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
            val activeSuffix = stringResource(R.string.vlm_profile_active_suffix)
            val tempNaText = stringResource(R.string.vlm_profile_temp_na)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(profile) }
            ) {
                SectionCard(
                    title = if (isActive) "${profile.label} $activeSuffix" else profile.label
                ) {
                    profile.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    val tempText = profile.parameterOverrides.temperature?.let { "%.2f".format(it) } ?: tempNaText
                    val effortText = profile.tokenPolicy.reasoningEffort?.let {
                        stringResource(R.string.vlm_profile_reasoning_format, it)
                    } ?: ""
                    Text(
                        text = stringResource(R.string.vlm_profile_model_format, profile.modelId),
                        style = MaterialTheme.typography.bodySmall
                    )
                    val streamingText = if (profile.streamingEnabled) {
                        stringResource(R.string.vlm_profile_streaming_on)
                    } else {
                        stringResource(R.string.vlm_profile_streaming_off)
                    }
                    Text(
                        text = stringResource(
                            R.string.vlm_profile_params_format,
                            tempText,
                            profile.tokenPolicy.maxTokens,
                            effortText,
                            streamingText
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
