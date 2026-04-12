package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.settings.OpenRouterCurrentKeyInfo
import com.owlitech.owli.assist.settings.OpenRouterKeyInfoErrorReason
import com.owlitech.owli.assist.settings.OpenRouterKeyInfoUiState
import com.owlitech.owli.assist.settings.VlmTransportMode
import java.text.NumberFormat

@Composable
fun OpenRouterKeyInfoScreen(
    state: OpenRouterKeyInfoUiState,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (state is OpenRouterKeyInfoUiState.Idle) {
            onRefresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.openrouter_key_info_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            stringResource(R.string.openrouter_key_info_note),
            style = MaterialTheme.typography.bodySmall
        )

        when (state) {
            is OpenRouterKeyInfoUiState.Idle,
            is OpenRouterKeyInfoUiState.Loading -> LoadingState(state)

            is OpenRouterKeyInfoUiState.Success -> SuccessState(
                sourceMode = state.result.sourceMode,
                info = state.result.info,
                onRefresh = onRefresh
            )

            is OpenRouterKeyInfoUiState.Error -> ErrorState(
                sourceMode = state.sourceMode,
                reason = state.reason,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun LoadingState(state: OpenRouterKeyInfoUiState) {
    val sourceMode = (state as? OpenRouterKeyInfoUiState.Loading)?.sourceMode
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sourceMode?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_source),
                value = keySourceLabel(it)
            )
        }
        CircularProgressIndicator()
        Text(
            stringResource(R.string.openrouter_key_info_loading),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SuccessState(
    sourceMode: VlmTransportMode,
    info: OpenRouterCurrentKeyInfo,
    onRefresh: () -> Unit
) {
    val numberFormatter = NumberFormat.getNumberInstance()
    numberFormatter.maximumFractionDigits = 2

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoItem(
            label = stringResource(R.string.openrouter_key_info_source),
            value = keySourceLabel(sourceMode)
        )
        info.label?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_label),
                value = it
            )
        }
        info.limitRemaining?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_limit_remaining),
                value = numberFormatter.format(it)
            )
        }
        info.limit?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_limit_total),
                value = numberFormatter.format(it)
            )
        }
        info.limitReset?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_limit_reset),
                value = it
            )
        }
        info.expiresAt?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_expires_at),
                value = it
            )
        }
        info.usage?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_usage_total),
                value = numberFormatter.format(it)
            )
        }
        info.usageDaily?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_usage_daily),
                value = numberFormatter.format(it)
            )
        }
        info.usageWeekly?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_usage_weekly),
                value = numberFormatter.format(it)
            )
        }
        info.usageMonthly?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_usage_monthly),
                value = numberFormatter.format(it)
            )
        }
        info.isFreeTier?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_free_tier),
                value = if (it) {
                    stringResource(R.string.openrouter_key_info_yes)
                } else {
                    stringResource(R.string.openrouter_key_info_no)
                }
            )
        }
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.openrouter_key_info_refresh))
        }
    }
}

@Composable
private fun ErrorState(
    sourceMode: VlmTransportMode?,
    reason: OpenRouterKeyInfoErrorReason,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sourceMode?.let {
            InfoItem(
                label = stringResource(R.string.openrouter_key_info_source),
                value = keySourceLabel(it)
            )
        }
        Text(
            text = when (reason) {
                OpenRouterKeyInfoErrorReason.BACKEND_MODE_ACTIVE -> stringResource(R.string.openrouter_key_info_error_backend_active)
                OpenRouterKeyInfoErrorReason.NO_ACTIVE_KEY -> stringResource(R.string.openrouter_key_info_error_no_active_key)
                OpenRouterKeyInfoErrorReason.UNAUTHORIZED -> stringResource(R.string.openrouter_key_info_error_unauthorized)
                OpenRouterKeyInfoErrorReason.NETWORK -> stringResource(R.string.openrouter_key_info_error_network)
                OpenRouterKeyInfoErrorReason.INVALID_RESPONSE -> stringResource(R.string.openrouter_key_info_error_invalid_response)
                OpenRouterKeyInfoErrorReason.UNKNOWN -> stringResource(R.string.openrouter_key_info_error_unknown)
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.openrouter_key_info_refresh))
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun keySourceLabel(sourceMode: VlmTransportMode): String {
    return when (sourceMode) {
        VlmTransportMode.BACKEND_MANAGED -> stringResource(R.string.openrouter_key_info_source_backend)
        VlmTransportMode.DIRECT_OPENROUTER_BYOK -> stringResource(R.string.openrouter_key_info_source_user_key)
        VlmTransportMode.EMBEDDED_DEBUG -> stringResource(R.string.openrouter_key_info_source_app_key)
    }
}
